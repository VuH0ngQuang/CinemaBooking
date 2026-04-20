import React, { useEffect } from 'react'
import { useParams } from 'react-router-dom'
import BlurCircle from '../components/BlurCircle'
import { Heart, PlayCircleIcon, X } from 'lucide-react'
import DateSelect from '../components/DateSelect'
import { useNavigate } from 'react-router-dom'
import Loading from '../components/Loading'

const getYoutubeIdFromUrl = (url) => {
  if (!url) return null

  try {
    const parsedUrl = new URL(url)
    const host = parsedUrl.hostname.replace('www.', '')

    if (host === 'youtu.be') {
      return parsedUrl.pathname.split('/').filter(Boolean)[0] || null
    }

    if (host.includes('youtube.com')) {
      if (parsedUrl.pathname === '/watch') {
        return parsedUrl.searchParams.get('v')
      }
      const parts = parsedUrl.pathname.split('/').filter(Boolean)
      const markerIndex = parts.findIndex((part) => part === 'embed' || part === 'shorts')
      if (markerIndex !== -1 && parts[markerIndex + 1]) {
        return parts[markerIndex + 1]
      }
    }
  } catch {
    const match = url.match(/(?:youtu\.be\/|v=|\/embed\/|\/shorts\/)([A-Za-z0-9_-]{11})/)
    return match ? match[1] : null
  }

  return null
}

const MovieDetails = () => {
  const navigate = useNavigate()
  const { id } = useParams()
  const [movie, setMovie] = React.useState(null)
  const [movies, setMovies] = React.useState([])
  const [dateTime, setDateTime] = React.useState({})
  const [isTrailerOpen, setIsTrailerOpen] = React.useState(false)
  const baseUrl = import.meta.env.VITE_BASE_URL

  useEffect(() => {
    try {
      const savedMovies = localStorage.getItem('movies')
      if (!savedMovies) {
        setMovie(null)
        return
      }

      const parsedMovies = JSON.parse(savedMovies)
      if (!Array.isArray(parsedMovies)) {
        setMovie(null)
        return
      }

      setMovies(parsedMovies)
      const selectedMovie = parsedMovies.find((item) => item.movie_id.toString() === id)
      setMovie(selectedMovie || null)
    } catch (error) {
      console.error('Failed to parse movies from localStorage:', error)
      setMovie(null)
    }
  }, [id])

  useEffect(() => {
    if (!id || !baseUrl) {
      setDateTime({})
      return
    }

    const fetchShowtimes = async () => {
      try {
        const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/showtimes/movie/${id}`)
        if (!response.ok) {
          throw new Error(`Failed to fetch showtimes: ${response.status}`)
        }

        const showtimes = await response.json()
        if (!Array.isArray(showtimes)) {
          setDateTime({})
          return
        }

        const groupedByDate = showtimes.reduce((accumulator, showtime) => {
          if (!showtime?.start_time) {
            return accumulator
          }

          const dateKey = showtime.start_time.split('T')[0]
          if (!accumulator[dateKey]) {
            accumulator[dateKey] = []
          }
          accumulator[dateKey].push(showtime)
          return accumulator
        }, {})

        localStorage.setItem(`showtimes_${id}`, JSON.stringify(showtimes))
        localStorage.setItem(`showtimes_by_date_${id}`, JSON.stringify(groupedByDate))
        setDateTime(groupedByDate)
      } catch (error) {
        console.error('Failed to fetch showtimes:', error)
        setDateTime({})
      }
    }

    fetchShowtimes()
  }, [id, baseUrl])

  const relatedMovies = movies
    .filter((item) => item.movie_id.toString() !== id)
    .slice(0, 4)

  const trailerYoutubeId = getYoutubeIdFromUrl(movie?.trailerUrl)

  useEffect(() => {
    if (!isTrailerOpen) return

    const onEsc = (event) => {
      if (event.key === 'Escape') {
        setIsTrailerOpen(false)
      }
    }

    window.addEventListener('keydown', onEsc)
    return () => window.removeEventListener('keydown', onEsc)
  }, [isTrailerOpen])

  return movie ? (
    <div className='px-6 md:px-16 lg:px-40 pt-30 md:pt-50'>
      <div className='flex flex-col md:flex-row gap-10 max-w-6xl mx-auto'>

        <img
          src={`https://minio.vuhongquang.com/cinemabooking/poster/vertical/${movie.movie_id}.jpg`}
          alt={movie.title}
          referrerPolicy="no-referrer"
          className='max-md:mx-auto rounded-xl h-104 max-w-70 object-cover'
        />

        <div className='relative flex flex-col gap-3'>
          <BlurCircle top="-100px" left="-100px" />

          <p className='text-primary'>{movie.age_rating || 'N/A'}</p>

          <h1 className='text-4xl font-semibold max-w-96 text-balance'>
            {movie.title}
          </h1>

          <p className='text-gray-400 mt-2 text-sm leading-tight max-w-xl'>
            {movie.description}
          </p>

          <p className='text-gray-400 text-sm'>
            {movie.duration_minutes ? `${movie.duration_minutes}m` : 'N/A'} •{" "}
            {movie.genre || 'Unknown'} •{" "}
            {movie.release_date ? new Date(movie.release_date).getFullYear() : 'N/A'}
          </p>

          <div className="flex items-center gap-4 mt-6">
            <button
              type="button"
              onClick={() => setIsTrailerOpen(true)}
              disabled={!trailerYoutubeId}
              className='flex items-center gap-2 px-7 py-3 text-sm bg-gray-800 hover:bg-gray-900 transition rounded-md font-medium cursor-pointer active:scale-95'
            >
              <PlayCircleIcon className='w-5 h-5' />
              Watch Trailer
            </button>
            <a href="#dateSelect" className='px-10 py-3 text-sm bg-primary hover:bg-primary-dull transition rounded-md font-medium cursor-pointer active:scale-95'>Buy Ticket</a>
            <button className='bg-gray-700 p-2.5 rounded-full transition cursor-pointer active:scale-95'>
              <Heart className='w-5 h-5' />
            </button>
          </div>
        </div>
      </div>

      <DateSelect dateTime={dateTime} id={id} />

      <p className='text-lg font-medium mt-20 mb-8'>You May Also Like</p>
      <div className='flex flex-wrap max-sm:justify-center gap-8'>
        {relatedMovies.map((relatedMovie) => (
          <div key={relatedMovie.movie_id} className='flex flex-col justify-between p-3 bg-gray-800 rounded-2xl hover:-translate-y-1 transition duration-300 w-66'>
            <img
              onClick={() => {
                navigate(`/movies/${relatedMovie.movie_id}`)
                scrollTo(0, 0)
              }}
              src={`https://minio.vuhongquang.com/cinemabooking/poster/vertical/${relatedMovie.movie_id}.jpg`}
              alt={relatedMovie.title}
              referrerPolicy="no-referrer"
              className='rounded-lg aspect-[2/3] w-full object-cover object-center cursor-pointer'
            />
            <p className='font-semibold mt-2 truncate'>{relatedMovie.title}</p>
            <p className='text-sm text-gray-400 mt-2'>
              {relatedMovie.release_date ? new Date(relatedMovie.release_date).getFullYear() : 'N/A'} - {relatedMovie.genre || 'Unknown'} - {relatedMovie.duration_minutes ? `${relatedMovie.duration_minutes}m` : 'N/A'}
            </p>
          </div>
        ))}
      </div>

      <div className='flex justify-center mt-20'>
        <button onClick={() => { navigate('/movies'); scrollTo(0, 0) }} className='px-10 py-3 text-sm bg-primary hover:bg-primary-dull transition rounded-md font-medium cursor-pointer'>Show more</button>
      </div>

      {isTrailerOpen && trailerYoutubeId && (
        <div
          className='fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4'
          onClick={() => setIsTrailerOpen(false)}
        >
          <div
            className='relative w-full max-w-5xl rounded-lg bg-black'
            onClick={(event) => event.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setIsTrailerOpen(false)}
              className='absolute -top-12 right-0 text-white hover:text-gray-300 transition'
            >
              <X className='w-7 h-7' />
            </button>

            <iframe
              src={`https://www.youtube.com/embed/${trailerYoutubeId}?autoplay=1&rel=0`}
              title={`${movie.title} trailer`}
              className='w-full rounded-lg'
              style={{ aspectRatio: '16 / 9' }}
              allow='autoplay; encrypted-media; picture-in-picture'
              allowFullScreen
            />
          </div>
        </div>
      )}
    </div>
  ) : <Loading />
}

export default MovieDetails
