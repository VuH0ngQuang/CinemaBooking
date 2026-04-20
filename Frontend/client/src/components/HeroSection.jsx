import React, { useEffect, useMemo, useRef, useState } from 'react'
import { ArrowRight, CalendarIcon, ClockIcon } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

const FALLBACK_IMAGE_URL = "https://minio.vuhongquang.com/cinemabooking/poster/horizontal/536046992.jpg"
const SLIDE_INTERVAL_MS = 3000
const FADE_DURATION_MS = 1000

const HeroSection = () => {
  const navigate = useNavigate()

  // dynamic object position
  const [position, setPosition] = useState("50% 40%")
  const [movies, setMovies] = useState([])
  const [activeIndex, setActiveIndex] = useState(0)
  const [displayedIndex, setDisplayedIndex] = useState(0)
  const [layerUrls, setLayerUrls] = useState([FALLBACK_IMAGE_URL, FALLBACK_IMAGE_URL])
  const [visibleLayer, setVisibleLayer] = useState(0)
  const [pendingLayer, setPendingLayer] = useState(null)
  const [isFading, setIsFading] = useState(false)
  const fadeTimeoutRef = useRef(null)

  useEffect(() => {
    try {
      const savedMovies = localStorage.getItem('movies')
      if (!savedMovies) {
        return
      }

      const parsedMovies = JSON.parse(savedMovies)
      if (Array.isArray(parsedMovies) && parsedMovies.length > 0) {
        setMovies(parsedMovies)
      }
    } catch (error) {
      console.error('Failed to parse movies from localStorage:', error)
    }
  }, [])

  useEffect(() => {
    if (movies.length <= 1) {
      return
    }

    const intervalId = setInterval(() => {
      setActiveIndex((currentIndex) => (currentIndex + 1) % movies.length)
    }, SLIDE_INTERVAL_MS)

    return () => clearInterval(intervalId)
  }, [movies])

  useEffect(() => {
    if (activeIndex === displayedIndex) {
      return
    }

    const candidateMovie = movies[activeIndex]
    const nextUrl = getImageUrl(candidateMovie)

    const targetLayer = visibleLayer === 0 ? 1 : 0
    if (layerUrls[targetLayer] === nextUrl) {
      setPendingLayer(targetLayer)
    } else {
      setLayerUrls((previous) => {
        const updated = [...previous]
        updated[targetLayer] = nextUrl
        return updated
      })
      setPendingLayer(targetLayer)
    }

    return () => {
      if (fadeTimeoutRef.current) {
        clearTimeout(fadeTimeoutRef.current)
        fadeTimeoutRef.current = null
      }
    }
  }, [activeIndex, displayedIndex, movies, visibleLayer, layerUrls])

  const startTransitionToPendingLayer = () => {
    if (pendingLayer === null) {
      return
    }

    setIsFading(true)
    if (fadeTimeoutRef.current) {
      clearTimeout(fadeTimeoutRef.current)
    }

    fadeTimeoutRef.current = setTimeout(() => {
      setVisibleLayer(pendingLayer)
      setDisplayedIndex(activeIndex)
      setPendingLayer(null)
      setIsFading(false)
      fadeTimeoutRef.current = null
    }, FADE_DURATION_MS)
  }

  const activeMovie = useMemo(() => {
    if (movies.length === 0) {
      return null
    }

    return movies[activeIndex] || movies[0]
  }, [movies, activeIndex])

  const getImageUrl = (movie) => movie?.movie_id
    ? `https://minio.vuhongquang.com/cinemabooking/poster/horizontal/${movie.movie_id}.jpg`
    : FALLBACK_IMAGE_URL

  const genre = activeMovie?.genre || 'Unknown'
  const releaseYear = activeMovie?.release_date
    ? new Date(activeMovie.release_date).getFullYear()
    : 'N/A'
  const duration = activeMovie?.duration_minutes ? `${activeMovie.duration_minutes}m` : 'N/A'
  const title = activeMovie?.title || 'Avengers: Endgame'

  const getLayerOpacityClass = (layerIndex) => {
    if (isFading && pendingLayer !== null) {
      return pendingLayer === layerIndex ? 'opacity-100' : 'opacity-0'
    }
    return visibleLayer === layerIndex ? 'opacity-100' : 'opacity-0'
  }

  const handleImageLoad = (e) => {
    const img = e.target
    const ratio = img.naturalWidth / img.naturalHeight

    // adjust cropping based on aspect ratio
    if (ratio > 2) {
      setPosition("50% 30%") // very wide images
    } else if (ratio < 1) {
      setPosition("50% 50%") // tall images
    } else {
      setPosition("50% 40%") // normal
    }
  }

  return (
    <div className='relative flex flex-col items-start justify-center gap-4 px-6 md:px-16 lg:px-36 h-screen overflow-hidden'>
      
      {/* Background Image */}
      <img
        src={layerUrls[0]}
        alt="movie background"
        referrerPolicy="no-referrer"
        onLoad={(event) => {
          handleImageLoad(event)
          if (pendingLayer === 0 && !isFading) {
            startTransitionToPendingLayer()
          }
        }}
        style={{ objectPosition: position }}
        className={`absolute inset-0 -z-10 w-full h-full object-cover transition-opacity duration-[900ms] ${getLayerOpacityClass(0)}`}
      />
      <img
        src={layerUrls[1]}
        alt="next movie background"
        referrerPolicy="no-referrer"
        onLoad={(event) => {
          handleImageLoad(event)
          if (pendingLayer === 1 && !isFading) {
            startTransitionToPendingLayer()
          }
        }}
        style={{ objectPosition: position }}
        className={`absolute inset-0 -z-10 w-full h-full object-cover transition-opacity duration-[900ms] ${getLayerOpacityClass(1)}`}
      />

      {/* Overlay */}
      <div className='absolute inset-0 -z-10 bg-black/60' />

      {/* Title */}
      <h1 className='max-w-xl text-5xl md:text-[70px] leading-tight font-semibold text-white text-balance break-words'>
        {title}
      </h1>

      {/* Meta */}
      <div className='flex items-center gap-4 text-gray-300'>
        <span>{genre}</span>

        <div className='flex items-center gap-1'>
          <CalendarIcon className='w-4 h-4' />
          {releaseYear}
        </div>

        <div className='flex items-center gap-1'>
          <ClockIcon className='w-4 h-4' />
          {duration}
        </div>
      </div>

      {/* Button */}
      <button
        onClick={() => navigate('/movies')}
        className='flex items-center gap-2 px-6 py-3 text-sm bg-primary hover:bg-primary-dull active:scale-95 transition-all duration-200 rounded-full font-medium text-white'
      >
        Explore Movies
        <ArrowRight className='w-5 h-5' />
      </button>
    </div>
  )
}

export default HeroSection