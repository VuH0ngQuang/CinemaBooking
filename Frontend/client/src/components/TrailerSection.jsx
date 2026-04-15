import React, { useEffect, useMemo, useState } from 'react'
import BlurCircle from './BlurCircle'
import { PlayCircleIcon } from 'lucide-react'

const getYoutubeIdFromUrl = (url) => {
  if (!url) {
    return null
  }

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

      const pathParts = parsedUrl.pathname.split('/').filter(Boolean)
      const embedIndex = pathParts.findIndex((part) => part === 'embed' || part === 'shorts')
      if (embedIndex !== -1 && pathParts[embedIndex + 1]) {
        return pathParts[embedIndex + 1]
      }
    }
  } catch (error) {
    const regexMatch = url.match(/(?:youtu\.be\/|v=|\/embed\/|\/shorts\/)([A-Za-z0-9_-]{11})/)
    return regexMatch ? regexMatch[1] : null
  }

  return null
}

const TrailerSection = () => {
  const [movies, setMovies] = useState([])
  const [currentTrailerId, setCurrentTrailerId] = useState(null)

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

  const trailers = useMemo(() => {
    return movies
      .map((movie) => {
        const trailerUrl = movie.trailerUrl || movie.trailer_url
        const youtubeId = getYoutubeIdFromUrl(trailerUrl)
        if (!youtubeId) {
          return null
        }

        return {
          movieId: movie.movie_id,
          title: movie.title,
          youtubeId,
          thumbnailUrl: `https://img.youtube.com/vi/${youtubeId}/maxresdefault.jpg`,
          embedUrl: `https://www.youtube.com/embed/${youtubeId}?rel=0`,
        }
      })
      .filter(Boolean)
  }, [movies])

  useEffect(() => {
    if (trailers.length === 0) {
      setCurrentTrailerId(null)
      return
    }

    setCurrentTrailerId((previousTrailerId) => {
      const hasPrevious = trailers.some((trailer) => trailer.movieId === previousTrailerId)
      return hasPrevious ? previousTrailerId : trailers[0].movieId
    })
  }, [trailers])

  const currentTrailer = useMemo(() => {
    if (trailers.length === 0) {
      return null
    }

    return trailers.find((trailer) => trailer.movieId === currentTrailerId) || trailers[0]
  }, [trailers, currentTrailerId])

  return (
    <div className='px-6 md:px-16 lg:px-24 xl:px-44 py-20 overflow-hidden'>
      <p className='text-gray-300 font-medium text-lg mb-10'>
        Trailers
      </p>

      <div className='mt-8'>
        <div className="relative mx-auto w-full max-w-[880px]">
          {currentTrailer ? (
            <iframe
              key={currentTrailer.youtubeId}
              src={currentTrailer.embedUrl}
              title={currentTrailer.title || 'Movie trailer'}
              className="relative z-10 w-full rounded-lg"
              style={{ aspectRatio: '1.78 / 1' }}
              allow="autoplay; encrypted-media; picture-in-picture"
              allowFullScreen
            />
          ) : (
            <div className='relative z-10 w-full rounded-lg bg-gray-800 flex items-center justify-center text-gray-400' style={{ aspectRatio: '1.85 / 1' }}>
              No trailers available
            </div>
          )}
          <BlurCircle top='-100px' right='-100px' />
        </div>

        <div className='group grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-8 mt-8 mx-auto w-full max-w-[880px]'>
            {trailers.map((trailer)=>(
                <div
                  key={trailer.movieId}
                  className='relative group-hover:not-hover:opacity-50 hover:-translate-y-1 duration-300 transition aspect-video cursor-pointer'
                  onClick={() => setCurrentTrailerId(trailer.movieId)}
                >
                    <img
                      src={trailer.thumbnailUrl}
                      alt={trailer.title || 'trailer'}
                      referrerPolicy="no-referrer"
                      className='rounded-lg w-full h-full object-cover brightness-75'
                    />
                    <PlayCircleIcon
                      strokeWidth={1.6}
                      className='absolute top-1/2 left-1/2 w-8 h-8 md:w-12 md:h-12 transform -translate-x-1/2 -translate-y-1/2'
                    />
                </div>
            ))}
        </div>
      </div>
    </div>
  )
}

export default TrailerSection
