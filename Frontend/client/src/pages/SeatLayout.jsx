import React, { useEffect, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Loading from '../components/Loading'
import { ArrowRightIcon, ClockIcon } from 'lucide-react'
import { assets } from '../assets/assets'
import isoTimeFormat from '../lib/isoTimeFormat'
import BlurCircle from '../components/BlurCircle'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'
import { buildApiUrl } from '../lib/api'

const createBooking = async (userId, showtimeId) => {
  const response = await fetch(buildApiUrl('/api/bookings'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ userId, showtimeId }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Create booking failed')
  }

  return response.json()
}

const addSeatToBooking = async (bookingId, seatId) => {
  const response = await fetch(buildApiUrl('/api/booking-seats'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ bookingId, seatId }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Add seat failed')
  }

  return response.json()
}

const removeSeatFromBooking = async (bookingId, seatId) => {
  const response = await fetch(buildApiUrl('/api/booking-seats'), {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ bookingId, seatId }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Remove seat failed')
  }
}

const getShowtimeSeatStatuses = async (showtimeId, bookingId) => {
  const params = new URLSearchParams()
  if (bookingId) params.set('bookingId', bookingId)

  const response = await fetch(
    buildApiUrl(`/api/showtimes/${showtimeId}/seats/status?${params.toString()}`),
    { credentials: 'include' }
  )

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Load seat statuses failed')
  }

  return response.json()
}

const resolveSeatActive = (seat) => {
  const candidates = [
    seat?._active,
    seat?.active,
    seat?.isActive,
    seat?.is_active,
  ]

  const firstBoolean = candidates.find((value) => typeof value === 'boolean')
  return firstBoolean ?? true
}

const SeatLayout = () => {
  const { id, date } = useParams()

  const [selectedSeats, setSelectedSeats] = React.useState([])
  const [selectedSeatIds, setSelectedSeatIds] = React.useState([])
  const [selectedTime, setSelectedTime] = React.useState(null)
  const [showtimesForDate, setShowtimesForDate] = React.useState([])
  const [screeningRoom, setScreeningRoom] = React.useState(null)
  const [roomSeats, setRoomSeats] = React.useState([])
  const [seatStatusBySeatId, setSeatStatusBySeatId] = React.useState({})
  const [bookingId, setBookingId] = React.useState(null)
  const [isPreparingBooking, setIsPreparingBooking] = React.useState(false)
  const [isLoadingRoom, setIsLoadingRoom] = React.useState(false)
  const [isLoadingSeats, setIsLoadingSeats] = React.useState(false)
  const [isSubmittingBooking, setIsSubmittingBooking] = React.useState(false)

  const navigate = useNavigate()
  const { user, openAuthModal } = useAuth()

  const requireAuth = () => {
    if (user) return true
    toast('Please login or register first')
    openAuthModal()
    return false
  }

  const selectedShowtime = useMemo(() => {
    return showtimesForDate.find((showtime) => showtime.start_time === selectedTime) || null
  }, [showtimesForDate, selectedTime])

  const refreshSeatStatuses = async (currentBookingId = bookingId) => {
    if (!selectedShowtime || !currentBookingId) return

    const statuses = await getShowtimeSeatStatuses(selectedShowtime.showtime_id, currentBookingId)

    const nextStatusMap = {}
    const nextSelectedSeatIds = []

    statuses.forEach((status) => {
      nextStatusMap[status.seatId] = status
      if (status.selectedByCurrentBooking) {
        nextSelectedSeatIds.push(status.seatId)
      }
    })

    setSeatStatusBySeatId(nextStatusMap)
    setSelectedSeatIds(nextSelectedSeatIds)
  }

  useEffect(() => {
    try {
      const savedShowtimes = localStorage.getItem(`showtimes_${id}`)
      if (!savedShowtimes) {
        setShowtimesForDate([])
        return
      }

      const parsedShowtimes = JSON.parse(savedShowtimes)
      if (!Array.isArray(parsedShowtimes)) {
        setShowtimesForDate([])
        return
      }

      const dateShowtimes = parsedShowtimes.filter((showtime) => {
        if (!showtime?.start_time) return false
        return showtime.start_time.split('T')[0] === date
      })

      setShowtimesForDate(dateShowtimes)
      setSelectedTime(null)
      setSelectedSeats([])
      setSelectedSeatIds([])
      setSeatStatusBySeatId({})
      setBookingId(null)
      setScreeningRoom(null)
      setRoomSeats([])
    } catch (error) {
      console.error('Failed to parse showtimes from localStorage:', error)
      setShowtimesForDate([])
    }
  }, [id, date])

  useEffect(() => {
    if (!selectedShowtime || !user) return

    let cancelled = false

    const prepareBookingAndSeatStatuses = async () => {
      try {
        if (!user?.user_id) {
          toast.error('User id not found')
          return
        }

        setIsPreparingBooking(true)
        const booking = await createBooking(user.user_id, selectedShowtime.showtime_id)
        const resolvedBookingId = booking.bookingId || booking.booking_id

        if (!resolvedBookingId) {
          throw new Error('Booking id not found in response')
        }

        const statuses = await getShowtimeSeatStatuses(selectedShowtime.showtime_id, resolvedBookingId)

        if (cancelled) return

        const nextStatusMap = {}
        const nextSelectedSeatIds = []
        statuses.forEach((status) => {
          nextStatusMap[status.seatId] = status
          if (status.selectedByCurrentBooking) {
            nextSelectedSeatIds.push(status.seatId)
          }
        })

        setBookingId(resolvedBookingId)
        setSeatStatusBySeatId(nextStatusMap)
        setSelectedSeatIds(nextSelectedSeatIds)
      } catch (error) {
        console.error('Failed to prepare booking for selected time:', error)
        if (!cancelled) {
          setBookingId(null)
          setSeatStatusBySeatId({})
          setSelectedSeatIds([])
          toast.error(error.message || 'Failed to prepare booking')
        }
      } finally {
        if (!cancelled) {
          setIsPreparingBooking(false)
        }
      }
    }

    prepareBookingAndSeatStatuses()

    return () => {
      cancelled = true
    }
  }, [selectedShowtime, user])

  useEffect(() => {
    if (!selectedTime) return

    const selectedShowtimeByTime = showtimesForDate.find((showtime) => showtime.start_time === selectedTime)

    const roomId =
      selectedShowtimeByTime?.screening_room_id ??
      selectedShowtimeByTime?.room_id ??
      selectedShowtimeByTime?.screeningRoomId

    if (!roomId) {
      setScreeningRoom(null)
      setRoomSeats([])
      return
    }

    const fetchRoomAndSeats = async () => {
      setIsLoadingRoom(true)
      setIsLoadingSeats(true)

      try {
        let room = null
        const cachedRoom = localStorage.getItem(`screening_room_${roomId}`)

        if (cachedRoom) {
          try {
            room = JSON.parse(cachedRoom)
          } catch {
            localStorage.removeItem(`screening_room_${roomId}`)
          }
        }

        if (!room) {
          const roomResponse = await fetch(buildApiUrl(`/api/screeningrooms/${roomId}`))

          if (!roomResponse.ok) {
            throw new Error(`Failed to fetch screening room: ${roomResponse.status}`)
          }

          room = await roomResponse.json()
          localStorage.setItem(`screening_room_${roomId}`, JSON.stringify(room))
        }

        setScreeningRoom(room)

        const seatsResponse = await fetch(buildApiUrl(`/api/seats/room/${roomId}`))

        if (!seatsResponse.ok) {
          throw new Error(`Failed to fetch seats: ${seatsResponse.status}`)
        }

        const seats = await seatsResponse.json()

        if (Array.isArray(seats)) {
          const normalizedSeats = seats.map((seat) => ({
            ...seat,
            seat_id: seat.seat_id,
            seat_row: seat.seat_row,
            seat_col: seat.seat_col,
            seat_label: seat.seat_label,
            is_active: resolveSeatActive(seat),
          }))

          setRoomSeats(normalizedSeats)
        } else {
          setRoomSeats([])
        }
      } catch (error) {
        console.error('Failed to fetch room or seats:', error)
        setScreeningRoom(null)
        setRoomSeats([])
      } finally {
        setIsLoadingRoom(false)
        setIsLoadingSeats(false)
      }
    }

    fetchRoomAndSeats()
  }, [selectedTime, showtimesForDate])

  useEffect(() => {
    if (!roomSeats.length || !selectedSeatIds.length) {
      setSelectedSeats([])
      return
    }

    const selectedSet = new Set(selectedSeatIds)
    setSelectedSeats(roomSeats.filter((seat) => selectedSet.has(seat.seat_id)))
  }, [roomSeats, selectedSeatIds])

  const groupedSeats = useMemo(() => {
    const grouped = {}

    roomSeats.forEach((seat) => {
      const rowNumber = seat.seat_row
      if (!grouped[rowNumber]) grouped[rowNumber] = []
      grouped[rowNumber].push(seat)
    })

    Object.keys(grouped).forEach((row) => {
      grouped[row].sort((a, b) => a.seat_col - b.seat_col)
    })

    return grouped
  }, [roomSeats])

  const orderedRows = useMemo(() => {
    return Object.keys(groupedSeats)
      .map(Number)
      .sort((a, b) => a - b)
  }, [groupedSeats])

  const isRealSeatSelected = (seatId) => {
    return selectedSeatIds.includes(seatId)
  }

  const handleRealSeatClick = async (seat) => {
    if (!requireAuth()) return

    if (!selectedTime) {
      return toast('Please select a time slot first')
    }

    if (!bookingId) {
      return toast('Preparing booking... please wait')
    }

    if (isPreparingBooking || isSubmittingBooking) return

    const seatActive = resolveSeatActive(seat)

    if (!seatActive) {
      return toast('This seat is inactive')
    }

    const seatStatus = seatStatusBySeatId[seat.seat_id]
    const selectedByCurrentBooking = Boolean(seatStatus?.selectedByCurrentBooking)
    const unavailableForCurrentUser =
      seatStatus &&
      (seatStatus.status === 'BOOKED' || (seatStatus.status === 'HELD' && !selectedByCurrentBooking))

    if (unavailableForCurrentUser) {
      return toast('This seat is already selected by another user')
    }

    const alreadySelected = selectedByCurrentBooking || selectedSeatIds.includes(seat.seat_id)

    if (!alreadySelected && selectedSeatIds.length > 4) {
      return toast('You can select maximum 5 seats')
    }

    try {
      if (alreadySelected) {
        await removeSeatFromBooking(bookingId, seat.seat_id)
      } else {
        await addSeatToBooking(bookingId, seat.seat_id)
      }

      await refreshSeatStatuses(bookingId)
    } catch (error) {
      console.error('Seat lock/unlock failed:', error)
      toast.error(error.message || 'Seat action failed')
      try {
        await refreshSeatStatuses(bookingId)
      } catch (refreshError) {
        console.error('Failed to refresh seat statuses:', refreshError)
      }
    }
  }

  const handleBooking = async () => {
    try {
      if (!requireAuth()) return

      if (!selectedShowtime) {
        return toast('Please select time first')
      }

      if (selectedSeatIds.length === 0) {
        return toast('Please select at least 1 seat')
      }

      setIsSubmittingBooking(true)

      if (!bookingId) {
        throw new Error('Booking is not ready yet')
      }

      navigate(`/payment/${bookingId}`)
    } catch (error) {
      console.error('Booking failed:', error)
      toast.error(error.message || 'Booking failed!')
    } finally {
      setIsSubmittingBooking(false)
    }
  }

  const renderRealSeatsByRow = (rowNumber) => {
    const seatsInRow = groupedSeats[rowNumber] || []

    return (
      <div key={rowNumber} className='flex gap-2 mt-2'>
        <div className='flex flex-wrap items-center justify-center gap-2'>
          {seatsInRow.map((seat) => {
            const seatDisplay = `${seat.seat_label}${seat.seat_col}`
            const selected = isRealSeatSelected(seat.seat_id)
            const seatActive = resolveSeatActive(seat)
            const seatStatus = seatStatusBySeatId[seat.seat_id]
            const selectedByCurrentBooking = Boolean(seatStatus?.selectedByCurrentBooking)
            const blockedByOthers =
              seatStatus &&
              (seatStatus.status === 'BOOKED' || (seatStatus.status === 'HELD' && !selectedByCurrentBooking))
            const disabled = !seatActive || blockedByOthers || !bookingId || isPreparingBooking

            return (
              <button
                key={seat.seat_id}
                onClick={() => handleRealSeatClick(seat)}
                disabled={disabled}
                className={`h-8 min-w-8 px-2 rounded border border-primary/60 cursor-pointer ${
                  selected ? 'bg-primary text-white' : ''
                } ${blockedByOthers ? 'bg-red-600 text-white border-red-500 cursor-not-allowed' : ''} ${
                  !seatActive ? 'opacity-40 cursor-not-allowed' : ''
                } ${!bookingId || isPreparingBooking ? 'opacity-60 cursor-not-allowed' : ''}`}
              >
                {seatDisplay}
              </button>
            )
          })}
        </div>
      </div>
    )
  }

  return (
    <div className='flex flex-col md:flex-row px-6 md:px-16 lg:px-40 py-30 md:pt-50'>
      <div
        className='w-60 bg-primary/10 border border-primary/20 rounded-lg py-10
        h-max md:sticky md:top-30'
      >
        <p className='text-lg font-semibold px-6'>Available Timings</p>
        <div>
          {showtimesForDate.length === 0 && (
            <p className='px-6 py-2 text-sm text-gray-400'>No timing available for this date</p>
          )}

          {showtimesForDate.map((item) => (
            <div
              key={item.showtime_id}
              onClick={() => {
                if (!requireAuth()) return
                setSeatStatusBySeatId({})
                setSelectedSeatIds([])
                setBookingId(null)
                setSelectedTime(item.start_time)
              }}
              className={`flex items-center gap-2 px-6 py-2 w-max rounded-r-md cursor-pointer transition ${
                selectedTime === item.start_time ? 'bg-primary text-white' : 'hover:bg-primary/20'
              }`}
            >
              <ClockIcon className='w-4 h-4' />
              <p className='text-sm'>{isoTimeFormat(item.start_time)}</p>
            </div>
          ))}
        </div>
      </div>

      <div className='relative flex-1 flex flex-col items-center max-md:mt-16'>
        <BlurCircle top='-100px' left='-100px' />
        <BlurCircle top='0px' left='0px' />
        <h1 className='text-2xl font-semibold mb-4'>Select your seat</h1>
        <img src={assets.screenImage} alt='screen' />
        <p className='text-gray-400 text-sm mb-6'>SCREEN SIDE</p>
        <div className='flex items-center gap-4 mb-4 text-xs text-gray-300'>
          <div className='flex items-center gap-2'>
            <span className='w-3 h-3 rounded border border-primary/60' />
            <span>Available</span>
          </div>
          <div className='flex items-center gap-2'>
            <span className='w-3 h-3 rounded bg-primary border border-primary/60' />
            <span>Your selection</span>
          </div>
          <div className='flex items-center gap-2'>
            <span className='w-3 h-3 rounded bg-red-600 border border-red-500' />
            <span>Unavailable</span>
          </div>
        </div>

        {isLoadingRoom || isLoadingSeats ? (
          <Loading />
        ) : screeningRoom ? (
          <div className='flex flex-col items-center mt-10 text-xs text-gray-300'>
            {orderedRows.map((rowNumber) => renderRealSeatsByRow(rowNumber))}
          </div>
        ) : (
          <p className='text-sm text-gray-400 mt-8'>Select a timing to load seat layout</p>
        )}

        {selectedSeats.length > 0 && (
          <div className='mt-6 text-sm text-gray-300 text-center'>
            Selected: {selectedSeats.map((seat) => `${seat.seat_label}${seat.seat_col}`).join(', ')}
          </div>
        )}

        <button
          onClick={handleBooking}
          disabled={isSubmittingBooking || isPreparingBooking || !bookingId}
          className='flex items-center gap-1 mt-20 px-10 py-3 text-sm
                bg-primary hover:bg-primary-dull transition rounded-full font-medium
                cursor-pointer active:scale-95 disabled:opacity-60 disabled:cursor-not-allowed'
        >
          {isPreparingBooking ? 'Preparing...' : isSubmittingBooking ? 'Processing...' : 'Proceed to Checkout'}
          <ArrowRightIcon strokeWidth={3} className='w-4 h-4' />
        </button>
      </div>
    </div>
  )
}

export default SeatLayout
