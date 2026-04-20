import React, { useEffect, useMemo, useState } from 'react'
import { ArrowRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import BlurCircle from './BlurCircle'

const FeatureSection = () => {
  const navigate = useNavigate()
  const [movies, setMovies] = useState([])

  useEffect(() => {
    try {
      const savedMovies = localStorage.getItem('movies')
      if (!savedMovies) {
        return
      }

      const parsedMovies = JSON.parse(savedMovies)
      if (Array.isArray(parsedMovies)) {
        setMovies(parsedMovies)
      }
    } catch (error) {
      console.error('Failed to parse movies from localStorage:', error)
    }
  }, [])

  const featuredMovies = useMemo(() => movies.slice(0, 4), [movies])

  return (
    <div className='px-6 md:px-16 lg:px-24 xl:px-44 overflow-hidden'>

        <div className='relative flex items-center justify-between pt-20 pb-10'>
            <BlurCircle top='0' right='-80px'/>
            <p className='text-gray-300 font-medium text-lg'>Now Showing</p>
            <button onClick={()=>navigate('/movies')} className='group flex items-center gap-2 text-sm text-gray-300 cursor-pointer'>
                View All <ArrowRight className='group-hover:translate-x-0.5 transition w-4.5 h-4.5'/>
            </button>
        </div>

        <div className='flex flex-wrap max-sm:justify-center gap-8 mt-8'>
            {featuredMovies.map((movie) => (
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

        <div className='flex justify-center mt-20'>
            <button onClick={()=>{navigate('/movies'); scrollTo(0,0)}}
            className='px-10 py-3 text-sm bg-primary hover:bg-primary-dull transition
            rounded-md font-medium cursor-pointer'>Show more</button>
        </div>
    </div>
  )
}

export default FeatureSection

