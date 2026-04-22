import React, { useEffect, useMemo, useState } from 'react'
import Loading from '../../components/Loading'
import Title from '../../components/admin/Title'
import { CheckIcon, DeleteIcon, StarIcon } from 'lucide-react'
import { kConverter } from '../../lib/kConverter'
import { getMovies, getScreeningRooms, createShowtime } from '../../lib/showtimeApi'
import { useAuth } from '../../context/AuthContext'

const AddShow = () => {
  const currency = 'VNĐ'
  const defaultBufferTime = 30

  const { token } = useAuth()

  const [nowPlayingMovies, setNowPlayingMovies] = useState([])
  const [screeningRooms, setScreeningRooms] = useState([])
  const [selectedMovie, setSelectedMovie] = useState(null)
  const [selectedRoom, setSelectedRoom] = useState('')
  const [dateTimeSelection, setDateTimeSelection] = useState({})
  const [dateTimeInput, setDateTimeInput] = useState('')
  const [showPrice, setShowPrice] = useState('')
  const [loading, setLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const fetchInitialData = async () => {
    try {
      setLoading(true)
      setError('')

      const [moviesData, roomsData] = await Promise.all([
        getMovies(),
        getScreeningRooms(),
      ])

      setNowPlayingMovies(Array.isArray(moviesData) ? moviesData : [])
      setScreeningRooms(Array.isArray(roomsData) ? roomsData : [])
    } catch (err) {
      setError(err.message || 'Failed to load movies and screening rooms.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchInitialData()
  }, [])

  const selectedMovieData = useMemo(() => {
    return nowPlayingMovies.find(
      (movie) => Number(movie.movie_id) === Number(selectedMovie)
    ) || null
  }, [nowPlayingMovies, selectedMovie])

  const handleDateTimeAdd = () => {
    if (!dateTimeInput) return

    const [date, time] = dateTimeInput.split('T')
    if (!date || !time) return

    setDateTimeSelection((prev) => {
      const times = prev[date] || []
      if (!times.includes(time)) {
        return { ...prev, [date]: [...times, time] }
      }
      return prev
    })

    setDateTimeInput('')
  }

  const handleRemoveTime = (date, time) => {
    setDateTimeSelection((prev) => {
      const filteredTimes = prev[date].filter((t) => t !== time)

      if (filteredTimes.length === 0) {
        const { [date]: _, ...rest } = prev
        return rest
      }

      return {
        ...prev,
        [date]: filteredTimes,
      }
    })
  }

  const buildEndTime = (startDateTime, durationMinutes) => {
    const start = new Date(startDateTime)
    const end = new Date(start.getTime() + Number(durationMinutes || 0) * 60 * 1000)

    const year = end.getFullYear()
    const month = String(end.getMonth() + 1).padStart(2, '0')
    const day = String(end.getDate()).padStart(2, '0')
    const hours = String(end.getHours()).padStart(2, '0')
    const minutes = String(end.getMinutes()).padStart(2, '0')
    const seconds = String(end.getSeconds()).padStart(2, '0')

    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
  }

  const buildStartTime = (date, time) => `${date}T${time}:00`

  const handleSubmit = async () => {
    try {
      setError('')
      setSuccess('')

      if (!selectedMovie) {
        setError('Please select a movie.')
        return
      }

      if (!selectedRoom) {
        setError('Please select a screening room.')
        return
      }

      if (!showPrice || Number(showPrice) <= 0) {
        setError('Please enter a valid show price.')
        return
      }

      if (!selectedMovieData?.duration_minutes) {
        setError('Selected movie does not have a valid duration.')
        return
      }

      const selectedEntries = Object.entries(dateTimeSelection)
      if (selectedEntries.length === 0) {
        setError('Please add at least one date and time.')
        return
      }

      const payloads = selectedEntries.flatMap(([date, times]) =>
        times.map((time) => {
          const start_time = buildStartTime(date, time)
          const end_time = buildEndTime(start_time, selectedMovieData.duration_minutes)

          return {
            status: 'SCHEDULED',
            start_time,
            end_time,
            buffer_time: Number(defaultBufferTime),
            movie_id: Number(selectedMovie),
            screening_room_id: Number(selectedRoom),
            seat_price: Number(showPrice),
          }
        })
      )

      setIsSubmitting(true)

      await Promise.all(payloads.map((payload) => createShowtime(payload, token)))

      setSuccess(`Created ${payloads.length} showtime(s) successfully.`)
      setSelectedMovie(null)
      setSelectedRoom('')
      setDateTimeSelection({})
      setDateTimeInput('')
      setShowPrice('')
    } catch (err) {
      setError(err.message || 'Failed to create showtime.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return !loading ? (
    <>
      <Title text1="Add" text2="Shows" />

      {error && (
        <p className="mt-6 text-sm text-red-400 break-words">{error}</p>
      )}

      {success && (
        <p className="mt-6 text-sm text-green-400">{success}</p>
      )}

      <p className="mt-10 text-lg font-medium">Now Playing Movies</p>

      <div className="overflow-x-auto pb-4">
        <div className="group flex flex-wrap gap-4 mt-4 w-max">
          {nowPlayingMovies.map((movie, index) => {
            const movieKey = movie.movie_id ?? movie.id ?? `movie-${index}`
            const movieId = Number(movie.movie_id ?? movie.id)

            return (
              <div
                key={movieKey}
                className="relative max-w-40 cursor-pointer group-hover:not-hover:opacity-40 hover:-translate-y-1 transition duration-300"
                onClick={() =>
                  setSelectedMovie((prev) =>
                    Number(prev) === movieId ? null : movieId
                  )
                }
              >
                <div className="relative rounded-lg overflow-hidden">
                  <img
                    src={movie.poster_path || movie.poster || movie.thumbnail || ''}
                    alt={movie.title}
                    className="w-full object-cover brightness-90"
                  />

                  <div className="text-sm flex items-center justify-between p-2 bg-black/70 w-full absolute bottom-0 left-0">
                    <p className="flex items-center gap-1 text-gray-400">
                      <StarIcon className="w-4 h-4 text-primary fill-primary" />
                      {movie.vote_average ? Number(movie.vote_average).toFixed(1) : 'N/A'}
                    </p>
                    <p className="text-gray-300">
                      {movie.vote_count ? `${kConverter(movie.vote_count)} Votes` : 'No votes'}
                    </p>
                  </div>
                </div>

                {Number(selectedMovie) === movieId && (
                  <div className="absolute top-2 right-2 flex items-center justify-center bg-primary h-6 w-6 rounded">
                    <CheckIcon className="w-4 h-4 text-white" strokeWidth={2.5} />
                  </div>
                )}

                <p className="font-medium truncate">{movie.title}</p>
                <p className="text-gray-400 text-sm">{movie.release_date}</p>
              </div>
            )
          })}
        </div>
      </div>

      <div className="mt-8">
        <label className="block text-sm font-medium mb-2">Screening Room</label>
        <select
          value={selectedRoom}
          onChange={(e) => setSelectedRoom(e.target.value)}
          className="border border-gray-600 px-3 py-2 rounded-md bg-transparent outline-none min-w-72"
        >
          <option value="">Select screening room</option>
          {screeningRooms.map((room, index) => {
            const roomId = room.screening_room_id ?? room.room_id ?? room.id
            const roomKey = roomId ?? `room-${index}`

            return (
              <option key={roomKey} value={roomId}>
                {room.room_name || room.name || `Room ${roomId}`}
              </option>
            )
          })}
        </select>
      </div>

      <div className="mt-8">
        <label className="block text-sm font-medium mb-2">Show Price</label>
        <div className="inline-flex items-center gap-2 border border-gray-600 px-3 py-2 rounded-md">
          <p className="text-gray-400 text-sm">{currency}</p>
          <input
            min={0}
            type="number"
            value={showPrice}
            onChange={(e) => setShowPrice(e.target.value)}
            placeholder="Enter show price"
            className="outline-none bg-transparent"
          />
        </div>
      </div>

      <div className="mt-6">
        <label className="block text-sm font-medium mb-2">Select Date and Time</label>
        <div className="inline-flex gap-5 border border-gray-600 p-1 pl-3 rounded-lg">
          <input
            type="datetime-local"
            value={dateTimeInput}
            onChange={(e) => setDateTimeInput(e.target.value)}
            className="outline-none rounded-md bg-transparent"
          />
          <button
            onClick={handleDateTimeAdd}
            className="bg-primary/80 text-white px-3 py-2 text-sm rounded-lg hover:bg-primary cursor-pointer"
          >
            Add Time
          </button>
        </div>
      </div>

      {Object.keys(dateTimeSelection).length > 0 && (
        <div className="mt-6">
          <h2 className="mb-2">Selected Date-Time</h2>
          <ul className="space-y-3">
            {Object.entries(dateTimeSelection).map(([date, times]) => (
              <li key={date}>
                <div className="font-medium">{date}</div>
                <div className="flex flex-wrap gap-2 mt-1 text-sm">
                  {times.map((time) => (
                    <div
                      key={`${date}-${time}`}
                      className="border border-primary px-2 py-1 flex items-center rounded"
                    >
                      <span>{time}</span>
                      <DeleteIcon
                        onClick={() => handleRemoveTime(date, time)}
                        width={15}
                        className="ml-2 text-red-500 hover:text-red-700 cursor-pointer"
                      />
                    </div>
                  ))}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      {selectedMovieData && (
        <div className="mt-6 text-sm text-gray-400">
          Duration: {selectedMovieData.duration_minutes} minutes
          <br />
          Buffer time: {defaultBufferTime} minutes
        </div>
      )}

      <button
        onClick={handleSubmit}
        disabled={isSubmitting}
        className="bg-primary text-white px-8 py-2 mt-6 rounded hover:bg-primary/90 transition-all cursor-pointer disabled:opacity-60"
      >
        {isSubmitting ? 'Creating Shows...' : 'Add Show'}
      </button>
    </>
  ) : (
    <Loading />
  )
}

export default AddShow