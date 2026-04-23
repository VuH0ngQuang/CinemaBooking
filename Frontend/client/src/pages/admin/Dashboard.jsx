import React, { useEffect, useMemo, useState } from 'react'
import Title from '../../components/admin/Title'
import {
  getAllBookings,
  getAllMoviesForAdmin,
  getAllShowtimesForAdmin,
  getAllTickets,
  getAllUsers,
} from '../../lib/adminManagementApi'

const formatCurrency = (value) => {
  const numericValue = Number(value || 0)

  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(numericValue)
}

const formatNumber = (value) => {
  return new Intl.NumberFormat('en-US').format(Number(value || 0))
}

const DashboardCard = ({ title, value, subtitle }) => {
  return (
    <div className='rounded-xl border border-primary/20 bg-primary/10 p-5'>
      <p className='text-sm text-gray-400'>{title}</p>
      <h3 className='mt-2 text-3xl font-bold text-white'>{value}</h3>
      {subtitle ? <p className='mt-2 text-xs text-gray-500'>{subtitle}</p> : null}
    </div>
  )
}

const Dashboard = () => {
  const [movies, setMovies] = useState([])
  const [showtimes, setShowtimes] = useState([])
  const [bookings, setBookings] = useState([])
  const [users, setUsers] = useState([])
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true)
        setError('')

        const [moviesData, showtimesData, bookingsData, usersData, ticketsData] =
          await Promise.all([
            getAllMoviesForAdmin(),
            getAllShowtimesForAdmin(),
            getAllBookings(),
            getAllUsers(),
            getAllTickets(),
          ])

        setMovies(Array.isArray(moviesData) ? moviesData : [])
        setShowtimes(Array.isArray(showtimesData) ? showtimesData : [])
        setBookings(Array.isArray(bookingsData) ? bookingsData : [])
        setUsers(Array.isArray(usersData) ? usersData : [])
        setTickets(Array.isArray(ticketsData) ? ticketsData : [])
      } catch (err) {
        setError(err.message || 'Failed to load dashboard data.')
      } finally {
        setLoading(false)
      }
    }

    fetchDashboardData()
  }, [])

  const metrics = useMemo(() => {
    const normalizedBookings = bookings.map((booking) => {
      const totalPrice = booking.total_price ?? booking.totalPrice ?? 0
      const bookingStatus =
        booking.booking_status ??
        booking.bookingStatus ??
        booking.status ??
        ''

      return {
        totalPrice: Number(totalPrice || 0),
        bookingStatus: String(bookingStatus).toUpperCase(),
      }
    })

    const totalRevenue = normalizedBookings
      .filter(
        (booking) =>
          booking.bookingStatus === 'PAID' ||
          booking.bookingStatus === 'CONFIRMED'
      )
      .reduce((sum, booking) => sum + booking.totalPrice, 0)

    const pendingBookings = normalizedBookings.filter(
      (booking) => booking.bookingStatus === 'PENDING'
    ).length

    const paidOrConfirmedBookings = normalizedBookings.filter(
      (booking) =>
        booking.bookingStatus === 'PAID' ||
        booking.bookingStatus === 'CONFIRMED'
    ).length

    const activeUsers = users.filter((user) => {
      const status = String(user.status ?? '').toUpperCase()
      return status === 'ACTIVE' || status === 'ENABLED' || status === '1'
    }).length

    const scheduledShowtimes = showtimes.filter((showtime) => {
      const status = String(showtime.status ?? '').toUpperCase()
      return status === 'SCHEDULED'
    }).length

    return {
      totalMovies: movies.length,
      totalShowtimes: showtimes.length,
      scheduledShowtimes,
      totalBookings: bookings.length,
      pendingBookings,
      paidOrConfirmedBookings,
      totalUsers: users.length,
      activeUsers,
      totalTickets: tickets.length,
      totalRevenue,
    }
  }, [bookings, movies, showtimes, tickets, users])

  return (
    <div className='space-y-6'>
      <Title text1='Admin' text2='Dashboard' />

      {loading && (
        <div className='rounded-xl border border-primary/20 bg-primary/10 p-4 text-sm text-gray-300'>
          Loading dashboard...
        </div>
      )}

      {error && (
        <div className='rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300'>
          {error}
        </div>
      )}

      {!loading && !error && (
        <>
          <div className='grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4'>
            <DashboardCard
              title='Total Movies'
              value={formatNumber(metrics.totalMovies)}
              subtitle='Movies currently stored in the system'
            />
            <DashboardCard
              title='Total Showtimes'
              value={formatNumber(metrics.totalShowtimes)}
              subtitle={`Scheduled: ${formatNumber(metrics.scheduledShowtimes)}`}
            />
            <DashboardCard
              title='Total Bookings'
              value={formatNumber(metrics.totalBookings)}
              subtitle={`Pending: ${formatNumber(metrics.pendingBookings)}`}
            />
            <DashboardCard
              title='Total Users'
              value={formatNumber(metrics.totalUsers)}
              subtitle={`Active: ${formatNumber(metrics.activeUsers)}`}
            />
            <DashboardCard
              title='Total Tickets'
              value={formatNumber(metrics.totalTickets)}
              subtitle='Issued tickets in the system'
            />
            <DashboardCard
              title='Revenue'
              value={formatCurrency(metrics.totalRevenue)}
              subtitle={`Paid/Confirmed bookings: ${formatNumber(metrics.paidOrConfirmedBookings)}`}
            />
          </div>

          <div className='grid grid-cols-1 lg:grid-cols-2 gap-4'>
            <div className='rounded-xl border border-primary/20 bg-primary/10 p-5'>
              <h3 className='text-lg font-semibold text-white'>Quick Summary</h3>
              <div className='mt-4 space-y-3 text-sm'>
                <div className='flex items-center justify-between border-b border-white/10 pb-2'>
                  <span className='text-gray-400'>Movies</span>
                  <span>{formatNumber(metrics.totalMovies)}</span>
                </div>
                <div className='flex items-center justify-between border-b border-white/10 pb-2'>
                  <span className='text-gray-400'>Showtimes</span>
                  <span>{formatNumber(metrics.totalShowtimes)}</span>
                </div>
                <div className='flex items-center justify-between border-b border-white/10 pb-2'>
                  <span className='text-gray-400'>Bookings</span>
                  <span>{formatNumber(metrics.totalBookings)}</span>
                </div>
                <div className='flex items-center justify-between border-b border-white/10 pb-2'>
                  <span className='text-gray-400'>Users</span>
                  <span>{formatNumber(metrics.totalUsers)}</span>
                </div>
                <div className='flex items-center justify-between border-b border-white/10 pb-2'>
                  <span className='text-gray-400'>Tickets</span>
                  <span>{formatNumber(metrics.totalTickets)}</span>
                </div>
                <div className='flex items-center justify-between'>
                  <span className='text-gray-400'>Revenue</span>
                  <span>{formatCurrency(metrics.totalRevenue)}</span>
                </div>
              </div>
            </div>

            <div className='rounded-xl border border-primary/20 bg-primary/10 p-5'>
              <h3 className='text-lg font-semibold text-white'>Booking Overview</h3>
              <div className='mt-4 space-y-3 text-sm'>
                <div className='flex items-center justify-between border-b border-white/10 pb-2'>
                  <span className='text-gray-400'>Pending Bookings</span>
                  <span>{formatNumber(metrics.pendingBookings)}</span>
                </div>
                <div className='flex items-center justify-between border-b border-white/10 pb-2'>
                  <span className='text-gray-400'>Paid / Confirmed</span>
                  <span>{formatNumber(metrics.paidOrConfirmedBookings)}</span>
                </div>
                <div className='flex items-center justify-between border-b border-white/10 pb-2'>
                  <span className='text-gray-400'>Scheduled Showtimes</span>
                  <span>{formatNumber(metrics.scheduledShowtimes)}</span>
                </div>
                <div className='flex items-center justify-between'>
                  <span className='text-gray-400'>Active Users</span>
                  <span>{formatNumber(metrics.activeUsers)}</span>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

export default Dashboard
