import React, { useEffect, useMemo, useState } from 'react'
import {
  MOVIES_CACHE_UPDATED_EVENT,
  readMoviesFromCache,
} from '../utils/moviesCache.js'
import BlurCircle from '../components/BlurCircle'
import { useNavigate } from 'react-router-dom'

const Movies = () => {
  const navigate = useNavigate()
  const [movies, setMovies] = useState([])

  useEffect(() => {
    const syncMovies = () => {
      setMovies(readMoviesFromCache())
    }

    syncMovies()
    window.addEventListener(MOVIES_CACHE_UPDATED_EVENT, syncMovies)

    return () => {
      window.removeEventListener(MOVIES_CACHE_UPDATED_EVENT, syncMovies)
    }
  }, [])

  const movieList = useMemo(() => movies, [movies])

  return movieList.length > 0 ? (
    <div className='relative my-40 mb-60 px-6 md:px-16 lg:px-40 xl:px-44
        overflow-hidden min-h-[80vh]'>

        <BlurCircle top="150px" left="0px"/>
        <BlurCircle bottom="50px" right="50px"/>
        <h1 className='text-lg font-medium my-4'>Now Showing</h1>
        <div className='flex flex-wrap max-sm:justify-center gap-8'>
            {movieList.map((movie) => (
                <div key={movie.movie_id} className='flex flex-col justify-between p-3 bg-gray-800 rounded-2xl hover:-translate-y-1 transition duration-300 w-66'>
                    <img
                        onClick={() => {
                          navigate(`/movies/${movie.movie_id}`)
                          scrollTo(0, 0)
                        }}
                        src={`https://minio.vuhongquang.com/cinemabooking/poster/vertical/${movie.movie_id}.jpg`}
                        alt={movie.title}
                        referrerPolicy="no-referrer"
                        className='rounded-lg aspect-[2/3] w-full object-cover object-center cursor-pointer'
                    />

                    <p className='font-semibold mt-2 truncate'>{movie.title}</p>

                    <p className='text-sm text-gray-400 mt-2'>
                        {movie.release_date ? new Date(movie.release_date).getFullYear() : 'N/A'} - {movie.genre || 'Unknown'} - {movie.duration_minutes ? `${movie.duration_minutes}m` : 'N/A'}
                    </p>

                    <div className='flex items-center justify-between mt-4 pb-3'>
                        <a
                            href={`/movies/${movie.movie_id}`}
                            className='px-4 py-2 text-xs bg-primary hover:bg-primary-dull transition rounded-dull font-medium cursor-pointer'
                        >
                            Buy Ticket
                        </a>
                    </div>
                </div>
            ))}
        </div>
    </div>
  ) : (
    <div className='flex flex-col items-center justify-center h-screen'>
        <h1 className='text-3xl font-bold text-center'>No movies available</h1>
    </div>
  )
}

export default Movies
