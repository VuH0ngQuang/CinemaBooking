import React, { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import BlurCircle from '../components/BlurCircle'
import Loading from '../components/Loading'
import { useAuth } from '../context/AuthContext'

const PaymentPage = () => {
  const { bookingId } = useParams()
  const navigate = useNavigate()
  const { token, openAuthModal } = useAuth()
  const baseUrl = import.meta.env.VITE_BASE_URL?.replace(/\/$/, '')

  const [booking, setBooking] = useState(null)
  const [checkoutUrl, setCheckoutUrl] = useState('')
  const [paymentId, setPaymentId] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isEmbeddedReady, setIsEmbeddedReady] = useState(false)

  const payosContainerRef = useRef(null)

  const requireAuth = () => {
    if (token) return true
    toast('Please login or register first')
    openAuthModal()
    return false
  }

  const clearPaymentSession = () => {
    sessionStorage.removeItem(`payment_id_${bookingId}`)
    sessionStorage.removeItem(`checkout_url_${bookingId}`)
  }

  const fetchBooking = async () => {
    const res = await fetch(`${baseUrl}/api/bookings/${bookingId}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    if (!res.ok) {
      throw new Error(`Failed to fetch booking: ${res.status}`)
    }

    return res.json()
  }

  const validateBookingStatusBeforePayment = (bookingData) => {
    const status = bookingData?.bookingStatus || bookingData?.booking_status

    if (['EXPIRED', 'CANCELLED', 'PAID', 'CONFIRMED'].includes(status)) {
      throw new Error(`Cannot pay for booking with status: ${status}`)
    }
  }

  const createPayment = async () => {
    const cachedPaymentId = sessionStorage.getItem(`payment_id_${bookingId}`)
    const cachedCheckoutUrl = sessionStorage.getItem(`checkout_url_${bookingId}`)

    if (cachedPaymentId && cachedCheckoutUrl) {
      setPaymentId(cachedPaymentId)
      setCheckoutUrl(cachedCheckoutUrl)
      return {
        paymentId: cachedPaymentId,
        checkoutUrl: cachedCheckoutUrl,
      }
    }

    const res = await fetch(`${baseUrl}/api/payments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        bookingId: Number(bookingId),
      }),
    })

    if (!res.ok) {
      let message = `Failed to create payment: ${res.status}`
      try {
        const errorText = await res.text()
        if (errorText) message = errorText
      } catch {
        // ignore
      }
      throw new Error(message)
    }

    const rawText = await res.text()

    if (!rawText || !rawText.trim()) {
      throw new Error('Payment API returned empty response')
    }

    let parsed = null

    if (rawText.startsWith('{') || rawText.startsWith('[')) {
      parsed = JSON.parse(rawText)
    } else if (rawText.startsWith('http://') || rawText.startsWith('https://')) {
      parsed = {
        checkoutUrl: rawText,
      }
    } else {
      throw new Error('Payment response is not valid JSON or URL')
    }

    const resolvedPaymentId =
      parsed?.paymentId || parsed?.payment_id || parsed?.id || ''
    const resolvedCheckoutUrl =
      parsed?.checkoutUrl || parsed?.checkout_url || parsed?.paymentUrl || parsed?.data || ''

    if (!resolvedCheckoutUrl) {
      throw new Error('checkoutUrl not found in payment response')
    }

    if (resolvedPaymentId) {
      sessionStorage.setItem(`payment_id_${bookingId}`, String(resolvedPaymentId))
      setPaymentId(String(resolvedPaymentId))
    }

    sessionStorage.setItem(`checkout_url_${bookingId}`, resolvedCheckoutUrl)
    setCheckoutUrl(resolvedCheckoutUrl)

    return {
      paymentId: resolvedPaymentId,
      checkoutUrl: resolvedCheckoutUrl,
    }
  }

  const markPaymentSuccess = async (resolvedPaymentId) => {
    const res = await fetch(`${baseUrl}/api/payments/${resolvedPaymentId}/mark-success`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    if (!res.ok) {
      let message = `Failed to mark payment success: ${res.status}`
      try {
        const errorText = await res.text()
        if (errorText) {
          message = errorText
        }
      } catch {
        // ignore
      }
      throw new Error(message)
    }

    return res.json()
  }

  const handlePaymentSuccess = async () => {
    try {
      const storedPaymentId = paymentId || sessionStorage.getItem(`payment_id_${bookingId}`)

      if (!storedPaymentId) {
        throw new Error('Payment ID not found in session')
      }

      await markPaymentSuccess(storedPaymentId)
      clearPaymentSession()

      toast.success('Payment completed successfully!')
      navigate('/my-bookings')
    } catch (error) {
      console.error(error)
      toast.error(error.message || 'Failed to finalize payment')
    }
  }

  const loadPayOSScript = () =>
    new Promise((resolve, reject) => {
      const existingScript = document.querySelector(
        'script[src="https://cdn.payos.vn/payos-checkout/v1/stable/payos-initialize.js"]'
      )

      if (existingScript) {
        resolve()
        return
      }

      const script = document.createElement('script')
      script.src = 'https://cdn.payos.vn/payos-checkout/v1/stable/payos-initialize.js'
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => reject(new Error('Failed to load PayOS SDK'))
      document.body.appendChild(script)
    })

  useEffect(() => {
    const init = async () => {
      try {
        if (!requireAuth()) return

        setIsLoading(true)

        const bookingData = await fetchBooking()
        validateBookingStatusBeforePayment(bookingData)
        setBooking(bookingData)

        await createPayment()
      } catch (error) {
        console.error(error)
        toast.error(error.message || 'Failed to load payment page')
      } finally {
        setIsLoading(false)
      }
    }

    if (bookingId && baseUrl && token) {
      init()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bookingId, baseUrl, token])

  useEffect(() => {
    const initEmbedded = async () => {
      try {
        if (!checkoutUrl || !payosContainerRef.current) return

        await loadPayOSScript()

        if (!window.PayOSCheckout || !window.PayOSCheckout.usePayOS) {
          throw new Error('PayOS SDK is not available')
        }

        window.PayOSCheckout.usePayOS({
          CHECKOUT_URL: checkoutUrl,
          embedded: true,
          container: payosContainerRef.current,
          onSuccess: async () => {
            await handlePaymentSuccess()
          },
          onCancel: () => {
            toast('Payment was cancelled')
          },
          onExit: () => {
            console.log('User exited payment')
          },
        })

        setIsEmbeddedReady(true)
      } catch (error) {
        console.error(error)
        toast.error(error.message || 'Failed to initialize embedded payment')
      }
    }

    initEmbedded()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [checkoutUrl])

  return (
    <div className='relative px-6 md:px-16 lg:px-40 py-30 md:pt-40 min-h-screen'>
      <BlurCircle top="-100px" left="-100px" />
      <BlurCircle top="100px" left="200px" />

      <div className='max-w-5xl mx-auto'>
        <h1 className='text-2xl md:text-3xl font-semibold mb-8 text-center'>
          Complete Your Payment
        </h1>

        {isLoading ? (
          <div className='flex justify-center mt-20'>
            <Loading />
          </div>
        ) : (
          <div className='grid lg:grid-cols-2 gap-8'>
            <div className='bg-primary/10 border border-primary/20 rounded-2xl p-6'>
              <h2 className='text-xl font-semibold mb-4'>Booking Information</h2>

              <div className='space-y-3 text-sm text-gray-300'>
                <div className='flex justify-between gap-4'>
                  <span>Booking ID</span>
                  <span className='text-white font-medium'>
                    {booking?.bookingId || booking?.booking_id}
                  </span>
                </div>

                <div className='flex justify-between gap-4'>
                  <span>Status</span>
                  <span className='text-white font-medium'>
                    {booking?.bookingStatus || booking?.booking_status || 'PENDING'}
                  </span>
                </div>

                <div className='flex justify-between gap-4'>
                  <span>Total Price</span>
                  <span className='text-white font-medium'>
                    {booking?.totalPrice || booking?.total_price || 0} VND
                  </span>
                </div>

                <div className='flex justify-between gap-4'>
                  <span>Showtime ID</span>
                  <span className='text-white font-medium'>
                    {booking?.showtimeId || booking?.showtime_id || 'N/A'}
                  </span>
                </div>

                <div className='flex justify-between gap-4'>
                  <span>Expires At</span>
                  <span className='text-white font-medium text-right'>
                    {booking?.expiredAt || booking?.expired_at || 'N/A'}
                  </span>
                </div>

                <div className='flex justify-between gap-4'>
                  <span>Payment ID</span>
                  <span className='text-white font-medium'>
                    {paymentId || 'N/A'}
                  </span>
                </div>
              </div>

              <button
                onClick={() => navigate('/my-bookings')}
                className='mt-8 w-full py-3 rounded-full border border-primary/40 hover:bg-primary/10 transition cursor-pointer'
              >
                Back to My Bookings
              </button>
            </div>

            <div className='bg-primary/10 border border-primary/20 rounded-2xl p-6'>
              <h2 className='text-xl font-semibold mb-4 text-center'>Scan QR / Open Payment</h2>

              {checkoutUrl ? (
                <>
                  <div className='flex justify-center'>
                    <img
                      src={`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(checkoutUrl)}`}
                      alt='Payment QR'
                      className='rounded-lg bg-white p-2'
                    />
                  </div>

                  <p className='text-xs text-gray-400 mt-4 text-center break-all'>
                    {checkoutUrl}
                  </p>
                </>
              ) : (
                <p className='text-sm text-gray-400 text-center'>
                  Payment link is being prepared...
                </p>
              )}

              <div className='mt-6'>
                <div
                  ref={payosContainerRef}
                  className='min-h-[320px] rounded-xl border border-primary/20'
                />
              </div>

              {!isEmbeddedReady && (
                <p className='text-sm text-gray-400 text-center mt-4'>
                  Initializing embedded payment...
                </p>
              )}

              <button
                onClick={handlePaymentSuccess}
                className='mt-6 w-full py-3 rounded-full bg-primary hover:bg-primary-dull transition font-medium cursor-pointer active:scale-95'
              >
                I Have Completed Payment
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default PaymentPage