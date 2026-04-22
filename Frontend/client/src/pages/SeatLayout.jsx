import React, { useEffect, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Loading from '../components/Loading'
import { ArrowRightIcon, ClockIcon } from 'lucide-react'
import { assets } from '../assets/assets'
import isoTimeFormat from '../lib/isoTimeFormat'
import BlurCircle from '../components/BlurCircle'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'

const createBooking = async (userId, showtimeId, token, baseUrl) => {
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/bookings`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      userId,
      showtimeId,
    }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Create booking failed')
  }

  return response.json()
}

const addSeatToBooking = async (bookingId, seatId, token, baseUrl) => {
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/booking-seats`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      bookingId,
      seatId,
    }),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Add seat failed')
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
  const baseUrl = import.meta.env.VITE_BASE_URL

  const [selectedSeats, setSelectedSeats] = React.useState([])
  const [selectedTime, setSelectedTime] = React.useState(null)
  const [showtimesForDate, setShowtimesForDate] = React.useState([])
  const [screeningRoom, setScreeningRoom] = React.useState(null)
  const [roomSeats, setRoomSeats] = React.useState([])
  const [isLoadingRoom, setIsLoadingRoom] = React.useState(false)
  const [isLoadingSeats, setIsLoadingSeats] = React.useState(false)
  const [isSubmittingBooking, setIsSubmittingBooking] = React.useState(false)

  const navigate = useNavigate()
  const { token, openAuthModal } = useAuth()

  const requireAuth = () => {
    if (token) return true
    toast('Please login or register first')
    openAuthModal()
    return false
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
      setScreeningRoom(null)
      setRoomSeats([])
    } catch (error) {
      console.error('Failed to parse showtimes from localStorage:', error)
      setShowtimesForDate([])
    }
  }, [id, date])

  useEffect(() => {
    if (!selectedTime || !baseUrl) return

    const selectedShowtime = showtimesForDate.find((showtime) => showtime.start_time === selectedTime)

    const roomId =
      selectedShowtime?.screening_room_id ??
      selectedShowtime?.room_id ??
      selectedShowtime?.screeningRoomId

    console.log('Selected showtime:', selectedShowtime)
    console.log('Resolved roomId:', roomId)

    if (!roomId) {
      console.error('Room id not found from selected showtime')
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
          const roomResponse = await fetch(
            `${baseUrl.replace(/\/$/, '')}/api/screeningrooms/${roomId}`
          )

          if (!roomResponse.ok) {
            throw new Error(`Failed to fetch screening room: ${roomResponse.status}`)
          }

          room = await roomResponse.json()
          localStorage.setItem(`screening_room_${roomId}`, JSON.stringify(room))
        }

        console.log('Fetched screening room:', room)
        setScreeningRoom(room)

        const seatsResponse = await fetch(
          `${baseUrl.replace(/\/$/, '')}/api/seats/room/${roomId}`
        )

        if (!seatsResponse.ok) {
          throw new Error(`Failed to fetch seats: ${seatsResponse.status}`)
        }

        const seats = await seatsResponse.json()
        console.log('Fetched room seats:', seats)

        if (Array.isArray(seats)) {
          const normalizedSeats = seats.map((seat) => ({
            ...seat,
            seat_id: seat.seat_id,
            seat_row: seat.seat_row,
            seat_col: seat.seat_col,
            seat_label: seat.seat_label,
            is_active: resolveSeatActive(seat),
          }))

          console.log('Normalized room seats:', normalizedSeats)
          console.log('First normalized seat:', normalizedSeats[0])
          console.table(normalizedSeats.slice(0, 5))

          setRoomSeats(normalizedSeats)
        } else {
          console.error('Seats response is not array:', seats)
          setRoomSeats([])
        }

        setSelectedSeats([])
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
  }, [selectedTime, showtimesForDate, baseUrl])

  const selectedShowtime = useMemo(() => {
    return showtimesForDate.find((showtime) => showtime.start_time === selectedTime) || null
  }, [showtimesForDate, selectedTime])

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
    return selectedSeats.some((seat) => seat.seat_id === seatId)
  }

  const handleRealSeatClick = (seat) => {
    if (!requireAuth()) return

    if (!selectedTime) {
      return toast('Please select a time slot first')
    }

    const seatActive = resolveSeatActive(seat)

    if (!seatActive) {
      return toast('This seat is inactive')
    }

    const alreadySelected = selectedSeats.some((selectedSeat) => selectedSeat.seat_id === seat.seat_id)

    if (!alreadySelected && selectedSeats.length > 4) {
      return toast('You can select maximum 5 seats')
    }

    setSelectedSeats((prev) => {
      if (prev.some((selectedSeat) => selectedSeat.seat_id === seat.seat_id)) {
        return prev.filter((selectedSeat) => selectedSeat.seat_id !== seat.seat_id)
      }
      return [...prev, seat]
    })
  }

  const handleBooking = async () => {
    try {
      if (!requireAuth()) return

      if (!baseUrl) {
        return toast.error('Base URL is missing')
      }

      if (!selectedShowtime) {
        return toast('Please select time first')
      }

      if (selectedSeats.length === 0) {
        return toast('Please select at least 1 seat')
      }

      const user = JSON.parse(localStorage.getItem('user'))
      if (!user) {
        return toast('User not found')
      }

      const userId = user.user_id
      const showtimeId = selectedShowtime.showtime_id

      if (!userId) {
        return toast.error('User id not found')
      }

      if (!showtimeId) {
        return toast.error('Showtime id not found')
      }

      setIsSubmittingBooking(true)

      const booking = await createBooking(userId, showtimeId, token, baseUrl)
      const bookingId = booking.bookingId || booking.booking_id

      if (!bookingId) {
        throw new Error('Booking id not found in response')
      }

      for (const seat of selectedSeats) {
        await addSeatToBooking(bookingId, seat.seat_id, token, baseUrl)
      }

      toast.success('Booking created successfully!')
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

            return (
              <button
                key={seat.seat_id}
                onClick={() => handleRealSeatClick(seat)}
                disabled={!seatActive}
                className={`h-8 min-w-8 px-2 rounded border border-primary/60 cursor-pointer ${
                  selected ? 'bg-primary text-white' : ''
                } ${!seatActive ? 'opacity-40 cursor-not-allowed' : ''}`}
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
            Selected:{' '}
            {selectedSeats.map((seat) => `${seat.seat_label}${seat.seat_col}`).join(', ')}
          </div>
        )}

        <button
          onClick={handleBooking}
          disabled={isSubmittingBooking}
          className='flex items-center gap-1 mt-20 px-10 py-3 text-sm
                bg-primary hover:bg-primary-dull transition rounded-full font-medium
                cursor-pointer active:scale-95 disabled:opacity-60 disabled:cursor-not-allowed'
        >
          {isSubmittingBooking ? 'Processing...' : 'Proceed to Checkout'}
          <ArrowRightIcon strokeWidth={3} className='w-4 h-4' />
        </button>
      </div>
    </div>
  )
}

export default SeatLayout