/*
 * k6 load / smoke test for CinemaBooking API.
 *
 * Run examples:
 *   # pure read smoke (no mutations)
 *   k6 run -e READS=true -e CREATES=false -e UPDATES=false -e DELETES=false k6/cinema-booking.test.js
 *
 *   # reads + creates only (skip PUT / DELETE)
 *   k6 run -e UPDATES=false -e DELETES=false k6/cinema-booking.test.js
 *
 *   # full CRUD cycle (default) - registers a test user, exercises booking,
 *   # then soft-deletes the user via its own JWT in teardown.
 *   k6 run k6/cinema-booking.test.js
 *
 *   # reuse a pre-existing user (skip register/login). Booking flow still runs.
 *   k6 run -e USER_ID=112345678 k6/cinema-booking.test.js
 *
 *   # override load profile
 *   k6 run -e VUS=20 -e DURATION=1m k6/cinema-booking.test.js
 *
 * Env vars:
 *   BASE_URL       default http://localhost:1325
 *   VUS            default 5
 *   RPS            default 0 (unlimited). Set >0 to throttle requests/sec.
 *   DURATION       default 30s
 *   READS          default true  - exercise GET endpoints
 *   CREATES        default true  - exercise POST endpoints (throw-away entities)
 *   UPDATES        default true  - exercise PUT  endpoints
 *   DELETES        default true  - exercise DELETE endpoints
 *   USER_ID        optional      - if set, skips register/login and uses this
 *                                   pre-existing user id for the booking flow
 *                                   (and skips user delete in teardown)
 *   USER_PASSWORD  default Password123! - password for the auto-registered user
 *   TEST_EMAIL     default test@test.com - shared login email for setup
 *   BOOKING_FLOW   default false - when true, runs booking endpoints
 *
 * Cleanup guarantee:
 *   - setup() creates fixture cinema/room/movie/showtime tagged with a unique
 *     runId. When USER_ID is not provided it also registers a fresh user via
 *     /api/auth/register, logs in to obtain a JWT, and resolves the numeric
 *     user id via GET /api/users/email/{email}.
 *   - All VU-created resources are tagged with the same runId so that
 *     teardown() can sweep them even if a VU iteration aborted mid-flow.
 *   - teardown() always runs. It deletes in dependency order:
 *        booking-seats -> bookings -> showtime -> movie -> room -> cinema
 *     and then re-scans list endpoints to purge any leftovers matching the
 *     runId tag, and finally soft-deletes the test user with its own JWT
 *     (DELETE /api/users/{id} only works for self or admin).
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ---------- config ----------
const BASE_URL = __ENV.BASE_URL || 'http://localhost:1325';
const VUS = parseInt(__ENV.VUS || '5', 10);
const RPS = parseInt(__ENV.RPS || '0', 10);
const DURATION = __ENV.DURATION || '30s';

const FLAG_READS   = (__ENV.READS   || 'true').toLowerCase() === 'true';
const FLAG_CREATES = (__ENV.CREATES || 'true').toLowerCase() === 'true';
const FLAG_UPDATES = (__ENV.UPDATES || 'true').toLowerCase() === 'true';
const FLAG_DELETES = (__ENV.DELETES || 'true').toLowerCase() === 'true';
const USER_ID      = __ENV.USER_ID ? __ENV.USER_ID.toString() : null;
const USER_PASSWORD = __ENV.USER_PASSWORD || 'Password123!';
const TEST_EMAIL = __ENV.TEST_EMAIL || 'test@test.com';
const FLAG_BOOKING_FLOW = (__ENV.BOOKING_FLOW || 'false').toLowerCase() === 'true';

// Unique tag for this run so teardown can sweep leftovers.
const RUN_ID = `k6-${Date.now()}-${Math.floor(Math.random() * 1e6)}`;

// ---------- k6 options ----------
export const options = {
    vus: VUS,
    duration: DURATION,
    ...(RPS > 0 ? { rps: RPS } : {}),
    // Statistics surfaced for every Trend metric (incl. http_req_duration).
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)'],
    thresholds: {
        http_req_failed:   ['rate<0.05'],    // <5% errors
        http_req_duration: ['p(95)<1500'],   // p95 under 1.5s
        checks:            ['rate>0.95'],
    },
};

// ---------- custom metrics ----------
const apiErrors       = new Counter('api_errors');
const mutationsDone   = new Counter('mutations_done');
const bookingsDone    = new Counter('bookings_done');

// ---------- helpers ----------
function req(method, path, body, tag, token) {
    const url = `${BASE_URL}${path}`;
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const params = { headers, tags: { name: tag || path } };
    const res = body !== undefined && body !== null
        ? http.request(method, url, JSON.stringify(body), params)
        : http.request(method, url, null, params);
    return res;
}

function isOk(res, expected) {
    const ok = Array.isArray(expected)
        ? expected.includes(res.status)
        : res.status === expected;
    if (!ok) apiErrors.add(1);
    return ok;
}

function safeJson(res) {
    try { return res.json(); } catch (_) { return null; }
}

function failDetail(res) {
    if (!res) return 'no response object';
    const body = String(res.body || '').trim();
    const compactBody = body.length > 400 ? `${body.slice(0, 400)}...` : body;
    const parts = [
        `status=${res.status}`,
        `error_code=${res.error_code || 'n/a'}`,
        `error=${res.error || 'n/a'}`,
    ];
    if (compactBody) parts.push(`body=${compactBody}`);
    return parts.join(' | ');
}

function isoDate(offsetMs) {
    return new Date(Date.now() + offsetMs).toISOString();
}

// ---------- setup: create fixtures ----------
export function setup() {
    console.log(`[setup] runId=${RUN_ID} baseUrl=${BASE_URL}`);
    const ctx = {
        runId: RUN_ID,
        userId: USER_ID,
        userToken: null,
        userEmail: null,
        userOwnedByTest: false, // true => teardown will soft-delete this user
    };

    // 0. Auto-register a test user when none was supplied. We need a real
    //    numeric user_id for the booking flow and a JWT for the authenticated
    //    DELETE /api/users/{id} cleanup.
    if (!ctx.userId) {
        const email = TEST_EMAIL;
        const password = USER_PASSWORD;

        // Prefer login first so repeated runs can reuse the same account.
        let r = req('POST', '/api/auth/login', { email, password }, 'setup:login-existing');
        if (!isOk(r, 200)) {
            r = req('POST', '/api/auth/register', {
                email: email,
                password: password,
                full_name: `k6 ${RUN_ID}`,
            }, 'setup:register');
            if (!isOk(r, [200, 201])) {
                throw new Error(`setup register failed: ${failDetail(r)}`);
            }

            r = req('POST', '/api/auth/login', { email, password }, 'setup:login');
            if (!isOk(r, 200)) {
                throw new Error(`setup login failed: ${failDetail(r)}`);
            }
        }

        // login returns the raw JWT as a JSON-encoded string
        ctx.userToken = (safeJson(r) || String(r.body).replace(/^"|"$/g, ''));

        // Resolve numeric user_id via the new email lookup endpoint
        r = req('GET', `/api/users/email/${encodeURIComponent(email)}`, null, 'setup:lookupUser');
        if (r.status !== 200) {
            throw new Error(`setup lookup user failed: ${failDetail(r)}`);
        }
        const u = safeJson(r) || {};
        ctx.userId = String(u.user_id || u.userId);
        ctx.userEmail = email;
        ctx.userOwnedByTest = true;
        console.log(`[setup] registered test user id=${ctx.userId} email=${email}`);
    }

    // 1. Cinema
    let res = req('POST', '/api/cinemas', {
        name: `Cinema ${RUN_ID}`,
        address: `k6 load test ${RUN_ID}`,
    }, 'setup:createCinema');
    if (res.status !== 201) throw new Error(`setup createCinema failed: ${res.status} ${res.body}`);
    ctx.cinemaId = safeJson(res).cinemas_id;

    // 2. Screening room (auto-creates seats: rows * cols)
    res = req('POST', '/api/screeningrooms', {
        room_name: `Room ${RUN_ID}`,
        amount_rows: 10,
        amount_cols: 15,
        cinema_id: ctx.cinemaId,
    }, 'setup:createRoom');
    if (res.status !== 201) throw new Error(`setup createRoom failed: ${res.status} ${res.body}`);
    ctx.roomId = safeJson(res).room_id;

    // 3. Movie
    res = req('POST', '/api/movies', {
        age_rating: 'PG13',
        release_date: isoDate(-86_400_000),
        title: `Movie ${RUN_ID}`,
        status: 'NOW_SHOWING',
        description: 'k6 load test fixture',
        genre: 'ACTION',
        duration_minutes: 120,
    }, 'setup:createMovie');
    if (res.status !== 201) throw new Error(`setup createMovie failed: ${res.status} ${res.body}`);
    ctx.movieId = safeJson(res).movie_id;

    // 4. Showtime
    res = req('POST', '/api/showtimes', {
        status: 'SCHEDULED',
        start_time: isoDate(3_600_000),
        end_time: isoDate(3_600_000 + 7_200_000),
        buffer_time: 15,
        movie_id: ctx.movieId,
        screening_room_id: ctx.roomId,
        seat_price: 100000,
    }, 'setup:createShowtime');
    if (res.status !== 201) throw new Error(`setup createShowtime failed: ${res.status} ${res.body}`);
    ctx.showtimeId = safeJson(res).showtime_id;

    // 5. Load seat ids for the booking flow
    res = req('GET', `/api/seats/room/${ctx.roomId}`, null, 'setup:listSeats');
    ctx.seatIds = [];
    if (res.status === 200) {
        const seats = safeJson(res) || [];
        ctx.seatIds = seats.map(s => s.seat_id || s.seatId).filter(Boolean);
    }

    console.log(`[setup] cinema=${ctx.cinemaId} room=${ctx.roomId} movie=${ctx.movieId} showtime=${ctx.showtimeId} seats=${ctx.seatIds.length}`);
    return ctx;
}

// ---------- default VU iteration ----------
export default function (ctx) {
    if (FLAG_READS) {
        group('reads', () => {
            const paths = [
                '/api/cinemas',
                `/api/cinemas/${ctx.cinemaId}`,
                '/api/screeningrooms',
                `/api/screeningrooms/${ctx.roomId}`,
                '/api/movies',
                `/api/movies/${ctx.movieId}`,
                '/api/showtimes',
                `/api/showtimes/${ctx.showtimeId}`,
                `/api/seats/room/${ctx.roomId}`,
                '/api/users',
            ];
            if (ctx.userId) paths.push(`/api/users/${ctx.userId}`);
            if (ctx.userEmail) paths.push(`/api/users/email/${encodeURIComponent(ctx.userEmail)}`);
            for (const p of paths) {
                const r = req('GET', p, null, `GET ${p.replace(/\d+/g, ':id')}`);
                check(r, { 'read 200': (x) => x.status === 200 }) || apiErrors.add(1);
            }
        });
    }

    // Throw-away cinema used to exercise create/update/delete in isolation
    // so they never conflict with the fixtures the other VUs are reading.
    let tempCinemaId = null;
    let tempMovieId  = null;

    if (FLAG_CREATES) {
        group('creates', () => {
            let r = req('POST', '/api/cinemas', {
                name: `temp-${ctx.runId}-${__VU}-${__ITER}`,
                address: 'k6 temp',
            }, 'POST /api/cinemas');
            if (isOk(r, 201)) {
                tempCinemaId = safeJson(r).cinemas_id;
                mutationsDone.add(1);
            }

            r = req('POST', '/api/movies', {
                age_rating: 'PG13',
                release_date: isoDate(-86_400_000),
                title: `tmp-${ctx.runId}-${__VU}-${__ITER}`,
                status: 'NOW_SHOWING',
                description: 'k6 temp',
                genre: 'ACTION',
                duration_minutes: 90,
            }, 'POST /api/movies');
            if (isOk(r, 201)) {
                tempMovieId = safeJson(r).movie_id;
                mutationsDone.add(1);
            }
        });
    }

    if (FLAG_UPDATES) {
        group('updates', () => {
            if (tempCinemaId) {
                const r = req('PUT', `/api/cinemas/${tempCinemaId}`, {
                    name: `temp-upd-${ctx.runId}-${__VU}-${__ITER}`,
                    address: 'k6 temp updated',
                }, 'PUT /api/cinemas/:id');
                isOk(r, 200) && mutationsDone.add(1);
            }
            if (tempMovieId) {
                const r = req('PUT', `/api/movies/${tempMovieId}`, {
                    age_rating: 'PG13',
                    release_date: isoDate(-86_400_000),
                    title: `tmp-upd-${ctx.runId}-${__VU}-${__ITER}`,
                    status: 'NOW_SHOWING',
                    description: 'k6 temp updated',
                    genre: 'ACTION',
                    duration_minutes: 95,
                }, 'PUT /api/movies/:id');
                isOk(r, 200) && mutationsDone.add(1);
            }
            // PUT /api/users/{id} doesn't require auth in the controller, but
            // only update the user we own to avoid touching unrelated users.
            if (ctx.userOwnedByTest && ctx.userId) {
                const r = req('PUT', `/api/users/${ctx.userId}`, {
                    full_name: `k6 ${ctx.runId} upd ${__VU}-${__ITER}`,
                }, 'PUT /api/users/:id');
                isOk(r, 200) && mutationsDone.add(1);
            }
        });
    }

    if (FLAG_DELETES) {
        group('deletes', () => {
            if (tempCinemaId) {
                const r = req('DELETE', `/api/cinemas/${tempCinemaId}`, null, 'DELETE /api/cinemas/:id');
                isOk(r, [200, 204]) && mutationsDone.add(1);
                tempCinemaId = null;
            }
            if (tempMovieId) {
                const r = req('DELETE', `/api/movies/${tempMovieId}`, null, 'DELETE /api/movies/:id');
                isOk(r, [200, 204]) && mutationsDone.add(1);
                tempMovieId = null;
            }
        });
    }

    // If CREATES is on but DELETES is off, the temp entities live until
    // teardown. They are tagged with runId so teardown will still sweep them.

    // Optional: booking flow (needs a real userId that already exists).
    if (FLAG_BOOKING_FLOW && ctx.userId && FLAG_CREATES && ctx.seatIds && ctx.seatIds.length > 0) {
        group('booking-flow', () => {
            const r = req('POST', '/api/bookings', {
                userId: Number(ctx.userId),
                showtimeId: ctx.showtimeId,
            }, 'POST /api/bookings');
            if (isOk(r, [200, 201])) {
                const booking = safeJson(r);
                bookingsDone.add(1);
                const bookingId = booking && (booking.bookingId || booking.booking_id);
                if (bookingId) {
                    // pick a random seat; collisions are OK in a load test
                    const seatId = ctx.seatIds[randomIntBetween(0, ctx.seatIds.length - 1)];
                    req('POST', '/api/booking-seats', {
                        bookingId: bookingId,
                        seatId: seatId,
                        price: 100000,
                    }, 'POST /api/booking-seats');
                    // teardown will clean up this booking + its seats
                }
            }
        });
    }

    sleep(randomIntBetween(1, 3) / 10); // 100-300ms think time
}

// ---------- teardown: wipe everything we created ----------
export function teardown(ctx) {
    console.log(`[teardown] starting cleanup for runId=${ctx.runId}`);
    let deleted = 0;
    let failed  = 0;

    const tryDelete = (path, label) => {
        const r = req('DELETE', path, null, `teardown:${label}`);
        if (r.status >= 200 && r.status < 300) {
            deleted++;
            return true;
        }
        if (r.status === 404) return true; // already gone
        console.warn(`[teardown] ${label} failed: ${r.status} ${String(r.body).slice(0, 200)}`);
        failed++;
        return false;
    };

    // 1. Bookings tied to our showtime (drop seats -> booking)
    const bRes = req('GET', '/api/bookings', null, 'teardown:listBookings');
    if (bRes.status === 200) {
        const bookings = safeJson(bRes) || [];
        const ours = bookings.filter(b => {
            const sid = b.showtimeId || b.showtime_id;
            return sid && Number(sid) === Number(ctx.showtimeId);
        });
        console.log(`[teardown] deleting ${ours.length} bookings`);

        // Delete booking seats first (per-booking is not exposed; use global list)
        const bsRes = req('GET', '/api/booking-seats', null, 'teardown:listBookingSeats');
        if (bsRes.status === 200) {
            const bookingIds = new Set(ours.map(b => Number(b.bookingId || b.booking_id)));
            const bseats = safeJson(bsRes) || [];
            const ourSeats = bseats.filter(bs => bookingIds.has(Number(bs.bookingId || bs.booking_id)));
            for (const bs of ourSeats) {
                const id = bs.bookingSeatId || bs.booking_seat_id;
                if (id) tryDelete(`/api/booking-seats/${id}`, 'deleteBookingSeat');
            }
        }

        for (const b of ours) {
            const id = b.bookingId || b.booking_id;
            if (id) tryDelete(`/api/bookings/${id}`, 'deleteBooking');
        }
    }

    // 2. Sweep temp cinemas / movies that VU iterations may have left behind
    //    (CREATES=true, DELETES=false, or mid-iter failures).
    const cRes = req('GET', '/api/cinemas', null, 'teardown:listCinemas');
    if (cRes.status === 200) {
        const cinemas = safeJson(cRes) || [];
        for (const c of cinemas) {
            const id = c.cinemas_id || c.cinemaId;
            const name = c.name || '';
            if (id && id !== ctx.cinemaId && name.includes(ctx.runId)) {
                tryDelete(`/api/cinemas/${id}`, 'deleteTempCinema');
            }
        }
    }

    const mRes = req('GET', '/api/movies', null, 'teardown:listMovies');
    if (mRes.status === 200) {
        const movies = safeJson(mRes) || [];
        for (const m of movies) {
            const id = m.movie_id || m.movieId;
            const title = m.title || '';
            if (id && id !== ctx.movieId && title.includes(ctx.runId)) {
                tryDelete(`/api/movies/${id}`, 'deleteTempMovie');
            }
        }
    }

    // 3. Fixtures (order matters: showtime -> movie -> room -> cinema)
    if (ctx.showtimeId) tryDelete(`/api/showtimes/${ctx.showtimeId}`, 'deleteShowtime');
    if (ctx.movieId)    tryDelete(`/api/movies/${ctx.movieId}`,       'deleteMovie');
    if (ctx.roomId)     tryDelete(`/api/screeningrooms/${ctx.roomId}`,'deleteRoom');
    if (ctx.cinemaId)   tryDelete(`/api/cinemas/${ctx.cinemaId}`,     'deleteCinema');

    // 4. Soft-delete the test user using its own JWT (DELETE /api/users/{id}
    //    requires self or admin). Skipped when the user was supplied via
    //    USER_ID env (we don't own that user).
    if (ctx.userOwnedByTest && ctx.userId && ctx.userToken) {
        const r = req('DELETE', `/api/users/${ctx.userId}`, null, 'teardown:deleteUser', ctx.userToken);
        if (r.status >= 200 && r.status < 300) {
            deleted++;
            console.log(`[teardown] soft-deleted test user id=${ctx.userId}`);
        } else {
            failed++;
            console.warn(`[teardown] deleteUser failed: ${r.status} ${String(r.body).slice(0, 200)}`);
        }
    }

    console.log(`[teardown] done. deleted=${deleted} failed=${failed}`);
    if (failed > 0) {
        console.warn(`[teardown] ${failed} objects could not be deleted; inspect DB manually if needed`);
    }
}

// ---------- summary report ----------
// Prints exactly: rps, error_rate, avg, min, max, med, p90, p95 — alongside
// the default k6 summary so the standard view is still available.
export function handleSummary(data) {
    const m = data.metrics || {};
    const fmt = (n, suffix = 'ms') => (n === undefined || n === null || Number.isNaN(n))
        ? '   n/a'
        : `${n.toFixed(2)}${suffix}`;

    const dur  = (m.http_req_duration && m.http_req_duration.values) || {};
    const reqs = (m.http_reqs        && m.http_reqs.values)         || {};
    const fail = (m.http_req_failed  && m.http_req_failed.values)   || {};

    const lines = [];
    lines.push('');
    lines.push('==================== CinemaBooking k6 summary ====================');
    lines.push(`requests total : ${reqs.count !== undefined ? reqs.count : 'n/a'}`);
    lines.push(`rps            : ${fmt(reqs.rate, '/s')}`);
    lines.push(`error_rate     : ${fail.rate !== undefined ? (fail.rate * 100).toFixed(2) + '%' : 'n/a'}`);
    lines.push('-- http_req_duration --');
    lines.push(`avg : ${fmt(dur.avg)}`);
    lines.push(`min : ${fmt(dur.min)}`);
    lines.push(`med : ${fmt(dur.med)}`);
    lines.push(`max : ${fmt(dur.max)}`);
    lines.push(`p90 : ${fmt(dur['p(90)'])}`);
    lines.push(`p95 : ${fmt(dur['p(95)'])}`);
    lines.push('==================================================================');
    lines.push('');

    const focused = lines.join('\n');
    const full = textSummary(data, { indent: ' ', enableColors: true });

    return {
        stdout: focused + '\n' + full + '\n',
        'k6-summary.json': JSON.stringify(data, null, 2),
    };
}
