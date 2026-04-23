import React, {useEffect, useState} from 'react'
import {useNavigate, useParams} from 'react-router-dom'
import toast from 'react-hot-toast'
import {usePayOS} from '@payos/payos-checkout'
import BlurCircle from '../components/BlurCircle'
import Loading from '../components/Loading'
import {useAuth} from '../context/AuthContext'
import { buildApiUrl } from '../lib/api'

const BookingInfoPanel = ({booking, paymentId, onBack}) => (
    <div className='bg-primary/10 border border-primary/20 rounded-2xl p-5 lg:h-fit'>
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
            onClick={onBack}
            className='mt-8 w-full py-3 rounded-full border border-primary/40 hover:bg-primary/10 transition cursor-pointer'
        >
            Back to My Bookings
        </button>
    </div>
)

const PaymentActionPanel = ({
    isEmbeddedReady,
    isOpen,
    onClose,
}) => (
    <div className='bg-primary/10 border border-primary/20 rounded-2xl p-6 lg:min-h-[760px]'>
        <h2 className='text-xl font-semibold mb-4 text-center'>Scan QR / Open Payment</h2>

        <div className='mt-6 min-h-[320px] rounded-xl border border-primary/20'>
            <div id='embedded-payment-container' className='h-[320px]'/>
        </div>

        {!isEmbeddedReady && (
            <p className='text-sm text-gray-400 text-center mt-4'>
                Initializing embedded payment...
            </p>
        )}

        {isOpen && (
            <button
                onClick={onClose}
                className='mt-4 w-full py-3 rounded-full border border-gray-500 text-gray-200 hover:bg-gray-700/20 transition cursor-pointer'
            >
                Close Embedded Payment
            </button>
        )}

        {isOpen && (
            <p className='text-xs text-gray-400 mt-3 text-center'>
                After completing payment, please wait 5-10 seconds for system sync.
            </p>
        )}

    </div>
)

const PaymentPage = () => {
    const {bookingId} = useParams()
    const navigate = useNavigate()
    const {user, openAuthModal} = useAuth()

    const [booking, setBooking] = useState(null)
    const [checkoutUrl, setCheckoutUrl] = useState('')
    const [paymentId, setPaymentId] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const [isEmbeddedReady, setIsEmbeddedReady] = useState(false)
    const [isOpen, setIsOpen] = useState(false)
    const [payOSConfig, setPayOSConfig] = useState({
        RETURN_URL: `${window.location.origin}/payment/`,
        ELEMENT_ID: 'embedded-payment-container',
        CHECKOUT_URL: null,
        embedded: true,
        onSuccess: () => {
            window.location.href = `${window.location.origin}/my-bookings`;
        },
        onCancel: () => {
            toast('Payment was cancelled')
        },
        onExit: () => {
            setIsOpen(false)
        },
    })

    const requireAuth = () => {
        if (user) return true
        toast('Please login or register first')
        openAuthModal()
        return false
    }

    const clearPaymentSession = () => {
        sessionStorage.removeItem(`payment_id_${bookingId}`)
        sessionStorage.removeItem(`checkout_url_${bookingId}`)
    }

    const fetchBooking = async () => {
        const res = await fetch(buildApiUrl(`/api/bookings/${bookingId}`), {
            method: 'GET',
            credentials: 'include',
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
        clearPaymentSession()

        const res = await fetch(buildApiUrl('/api/payments'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
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
            parsed = { checkoutUrl: rawText }
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
        setPayOSConfig((oldConfig) => ({
            ...oldConfig,
            CHECKOUT_URL: resolvedCheckoutUrl,
        }))

        return {
            paymentId: resolvedPaymentId,
            checkoutUrl: resolvedCheckoutUrl,
        }
    }

    const markPaymentSuccess = async (resolvedPaymentId) => {
        const res = await fetch(buildApiUrl(`/api/payments/${resolvedPaymentId}/mark-success`), {
            method: 'POST',
            credentials: 'include',
        })

        if (!res.ok) {
            let message = `Failed to mark payment success: ${res.status}`
            try {
                const errorText = await res.text()
                if (errorText) message = errorText
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

    const {open, exit} = usePayOS({
        ...payOSConfig,
        onSuccess: async () => {
            await handlePaymentSuccess()
            setIsOpen(false)
        },
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

        if (bookingId && user) {
            init()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [bookingId, user])

    useEffect(() => {
        if (!payOSConfig.CHECKOUT_URL || isLoading) return

        const runOpen = () => {
            const container = document.getElementById('embedded-payment-container')
            if (!container) return

            try {
                if (isOpen) {
                    exit()
                }
                open()
                setIsOpen(true)
                setIsEmbeddedReady(true)
            } catch (error) {
                console.error(error)
                toast.error(error.message || 'Failed to initialize embedded payment')
            }
        }

        const frame = window.requestAnimationFrame(runOpen)
        return () => window.cancelAnimationFrame(frame)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [payOSConfig.CHECKOUT_URL, isLoading])

    useEffect(() => {
        return () => {
            if (isOpen) {
                try {
                    exit()
                } catch {
                    // Ignore PayOS cleanup race conditions on unmount.
                }
            }
        }
    }, [exit, isOpen])

    return (
        <div className='relative px-6 md:px-16 lg:px-40 py-30 md:pt-40 min-h-screen'>
            <BlurCircle top="-100px" left="-100px"/>
            <BlurCircle top="100px" left="200px"/>

            <div className='max-w-6xl mx-auto'>
                <h1 className='text-2xl md:text-3xl font-semibold mb-8 text-center'>
                    Checkout
                </h1>

                {isLoading ? (
                    <div className='flex justify-center mt-20'>
                        <Loading/>
                    </div>
                ) : (
                    <div className='grid lg:grid-cols-3 gap-8 items-start'>
                        <div className='lg:col-span-2'>
                            <PaymentActionPanel
                                isEmbeddedReady={isEmbeddedReady}
                                isOpen={isOpen}
                                onClose={() => {
                                    setIsOpen(false)
                                    exit()
                                }}
                            />
                        </div>
                        <div className='lg:col-span-1'>
                            <BookingInfoPanel
                                booking={booking}
                                paymentId={paymentId}
                                onBack={() => navigate('/my-bookings')}
                            />
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}

export default PaymentPage
