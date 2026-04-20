import React, { useEffect, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import Loading from '../components/Loading'
import { ArrowRightIcon, ClockIcon } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { assets } from '../assets/assets'
import isoTimeFormat from '../lib/isoTimeFormat'
import BlurCircle from '../components/BlurCircle'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'

const SeatLayout = () => {
  const { id, date } = useParams()
  const baseUrl = import.meta.env.VITE_BASE_URL

  const [selectedSeats, setSelectedSeats] = React.useState([])
  const [selectedTime, setSelectedTime] = React.useState(null)
  const [showtimesForDate, setShowtimesForDate] = React.useState([])
  const [screeningRoom, setScreeningRoom] = React.useState(null)
  const [isLoadingRoom, setIsLoadingRoom] = React.useState(false)

  const navigate = useNavigate()
  const { token, openAuthModal } = useAuth()

  const requireAuth = () => {
    if (token) {
      return true
    }

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
        if (!showtime?.start_time) {
          return false
        }
        return showtime.start_time.split('T')[0] === date
      })

      setShowtimesForDate(dateShowtimes)
      setSelectedTime(null)
      setSelectedSeats([])
      setScreeningRoom(null)
    } catch (error) {
      console.error('Failed to parse showtimes from localStorage:', error)
      setShowtimesForDate([])
    }
  }, [id, date])

  useEffect(() => {
    if (!selectedTime || !baseUrl) {
      return
    }

    const selectedShowtime = showtimesForDate.find((showtime) => showtime.start_time === selectedTime)
    const roomId = selectedShowtime?.screening_room_id
    if (!roomId) {
      setScreeningRoom(null)
      return
    }

    const cachedRoom = localStorage.getItem(`screening_room_${roomId}`)
    if (cachedRoom) {
      try {
        setScreeningRoom(JSON.parse(cachedRoom))
        return
      } catch {
        localStorage.removeItem(`screening_room_${roomId}`)
      }
    }

    const fetchScreeningRoom = async () => {
      setIsLoadingRoom(true)
      try {
        const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/screeningrooms/${roomId}`)
        if (!response.ok) {
          throw new Error(`Failed to fetch screening room: ${response.status}`)
        }

        const room = await response.json()
        setScreeningRoom(room)
        localStorage.setItem(`screening_room_${roomId}`, JSON.stringify(room))
      } catch (error) {
        console.error('Failed to fetch screening room:', error)
        setScreeningRoom(null)
      } finally {
        setIsLoadingRoom(false)
      }
    }

    fetchScreeningRoom()
  }, [selectedTime, showtimesForDate, baseUrl])

  const handleSeatClick = (seatId) => {
    if (!requireAuth()) {
      return
    }

    if (!selectedTime) {
      return toast('Please select a time slot first')
    }
    if (!selectedSeats.includes(seatId) && selectedSeats.length > 4) {
      return toast('You can select maximum 5 seats')
    }

    setSelectedSeats((prev) =>
      prev.includes(seatId) ? prev.filter((seat) => seat !== seatId) : [...prev, seatId],
    )
  }

  const rowLabels = useMemo(() => {
    const totalRows = screeningRoom?.amount_rows || 0
    return Array.from({ length: totalRows }, (_, rowIndex) =>
      String.fromCharCode(65 + (rowIndex % 26)) + (rowIndex >= 26 ? Math.floor(rowIndex / 26) : ''),
    )
  }, [screeningRoom])

  const renderSeats = (rowLabel, count) => (
    <div key={rowLabel} className='flex gap-2 mt-2'>
      <div className='flex flex-wrap items-center justify-center gap-2'>
        {Array.from({ length: count }, (_, index) => {
          const seatId = `${rowLabel}${index + 1}`
          return (
            <button
              key={seatId}
              onClick={() => handleSeatClick(seatId)}
              className={`h-8 w-8 rounded border border-primary/60 cursor-pointer ${
                selectedSeats.includes(seatId) ? 'bg-primary text-white' : ''
              }`}
            >
              {seatId}
            </button>
          )
        })}
      </div>
    </div>
  )

  return (
    <div className='flex flex-col md:flex-row px-6 md:px-16 lg:px-40 py-30 md:pt-50'>
      <div className='w-60 bg-primary/10 border border-primary/20 rounded-lg py-10
        h-max md:sticky md:top-30'>
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
        <BlurCircle top="-100px" left="-100px" />
        <BlurCircle top="0px" left="0px" />
        <h1 className='text-2xl font-semibold mb-4'>Select your seat</h1>
        <img src={assets.screenImage} alt="screen" />
        <p className='text-gray-400 text-sm mb-6'>SCREEN SIDE</p>

        {isLoadingRoom ? (
          <Loading />
        ) : screeningRoom ? (
          <div className='flex flex-col items-center mt-10 text-xs text-gray-300'>
            {rowLabels.map((rowLabel) => renderSeats(rowLabel, screeningRoom.amount_cols))}
          </div>
        ) : (
          <p className='text-sm text-gray-400 mt-8'>Select a timing to load seat layout</p>
        )}

        <button onClick={() => {
          if (!requireAuth()) return
          navigate('/my-bookings')
        }} className='flex items-center gap-1 mt-20 px-10 py-3 text-sm
                bg-primary hover:bg-primary-dull transition rounded-full font-medium
                cursor-pointer active:scale-95'>
          Proceed to Checkout
          <ArrowRightIcon strokeWidth={3} className='w-4 h-4' />
        </button>
      </div>
    </div>
  )
}

export default SeatLayout
