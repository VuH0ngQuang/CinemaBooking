import React, { useEffect, useState } from 'react'
import Loading from '../components/Loading'
import BlurCircle from '../components/BlurCircle'
import { dateFormat } from '../lib/dateFormat'
import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'

const MyBookings = () => {
  const currency = 'VNĐ'
  const baseUrl = import.meta.env.VITE_BASE_URL?.replace(/\/$/, '')

  const { token, openAuthModal } = useAuth()
  const navigate = useNavigate()

  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)

  const requireAuth = () => {
    if (token) return true
    toast('Please login or register first')
    openAuthModal?.()
    return false
  }

  const getMyBookings = async () => {
    try {
      if (!requireAuth()) {
        setLoading(false)
        return
      }

      const response = await fetch(`${baseUrl}/api/bookings/my`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      if (!response.ok) {
        throw new Error(`Failed to fetch bookings: ${response.status}`)
      }

      const data = await response.json()

      const validBookings = (data || []).filter((b) => {
        const status = b.bookingStatus || b.booking_status
        return ['PAID', 'CONFIRMED'].includes(status)
      })

      setBookings(validBookings)
    } catch (error) {
      console.error('Failed to load bookings:', error)
      toast.error(error.message || 'Failed to load bookings')
      setBookings([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    getMyBookings()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  const resolveBooking = (item) => ({
    id: item.bookingId || item.booking_id,
    status: item.bookingStatus || item.booking_status,
    total: item.totalPrice || item.total_price || 0,
    showtimeId: item.showtimeId || item.showtime_id,
    createdAt: item.createdAt || item.created_at,
    confirmedAt: item.confirmedAt || item.confirmed_at,
    expiredAt: item.expiredAt || item.expired_at,
  })

  if (loading) {
    return <Loading />
  }

  return (
    <div className='relative px-6 md:px-16 lg:px-40 pt-30 md:pt-40 min-h-[80vh]'>
      <BlurCircle top="100px" left="100px" />
      <div>
        <BlurCircle bottom="0px" left="600px" />
      </div>

      <h1 className='text-lg font-semibold mb-4'>My Bookings</h1>

      {bookings.length === 0 ? (
        <div className='bg-primary/8 border border-primary/20 rounded-lg mt-4 p-6 max-w-3xl text-gray-300'>
          No bookings found
        </div>
      ) : (
        bookings.map((item, index) => {
          const booking = resolveBooking(item)

          return (
            <div
              key={booking.id || index}
              className='flex flex-col md:flex-row justify-between bg-primary/8 border border-primary/20 rounded-lg mt-4 p-4 max-w-3xl'
            >
              <div className='flex flex-col gap-2'>
                <p className='text-lg font-semibold'>Booking #{booking.id}</p>

                <p className='text-sm'>
                  <span className='text-gray-400'>Status: </span>
                  {booking.status}
                </p>

                <p className='text-sm'>
                  <span className='text-gray-400'>Showtime ID: </span>
                  {booking.showtimeId ?? 'N/A'}
                </p>

                <p className='text-sm'>
                  <span className='text-gray-400'>Created At: </span>
                  {booking.createdAt ? dateFormat(booking.createdAt) : 'N/A'}
                </p>

                <p className='text-sm'>
                  <span className='text-gray-400'>Confirmed At: </span>
                  {booking.confirmedAt ? dateFormat(booking.confirmedAt) : 'Not confirmed yet'}
                </p>
              </div>

              <div className='flex flex-col md:items-end md:text-right justify-between mt-4 md:mt-0'>
                <p className='text-2xl font-semibold mb-3'>
                  {booking.total} {currency}
                </p>

                <div className='flex flex-col gap-2'>
                  <button
                    onClick={() => navigate(`/booking/${booking.id}`)}
                    className='bg-primary px-4 py-2 text-sm rounded-full font-medium cursor-pointer'
                  >
                    View Details
                  </button>
                </div>
              </div>
            </div>
          )
        })
      )}
    </div>
  )
}

export default MyBookings