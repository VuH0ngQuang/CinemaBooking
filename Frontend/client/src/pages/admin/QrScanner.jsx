import React, { useState } from 'react'
import { QrReader } from 'react-qr-reader'
import toast from 'react-hot-toast'
import BlurCircle from '../../components/BlurCircle'
import { useAuth } from '../../context/AuthContext'

const QrScanner = () => {
  const baseUrl = import.meta.env.VITE_BASE_URL?.replace(/\/$/, '')
  const { token, openAuthModal } = useAuth()

  const [scannedText, setScannedText] = useState('')
  const [bookingCode, setBookingCode] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const [lastScannedValue, setLastScannedValue] = useState('')

  const requireAuth = () => {
    if (token) return true
    toast('Please login first')
    openAuthModal()
    return false
  }

  const extractBookingCode = (rawValue) => {
    if (!rawValue) return ''

    // 1) Nếu QR là plain text luôn
    if (
      typeof rawValue === 'string' &&
      !rawValue.trim().startsWith('{') &&
      !rawValue.trim().startsWith('[')
    ) {
      return rawValue.trim()
    }

    // 2) Nếu QR là JSON string
    try {
      const parsed = typeof rawValue === 'string' ? JSON.parse(rawValue) : rawValue

      return (
        parsed?.bookingCode ||
        parsed?.booking_code ||
        parsed?.code ||
        parsed?.bookingId ||
        parsed?.booking_id ||
        ''
      )
    } catch {
      return typeof rawValue === 'string' ? rawValue.trim() : ''
    }
  }

  const validateBookingCode = async (resolvedBookingCode) => {
    const res = await fetch(`${baseUrl}/api/tickets/validate-booking`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        bookingCode: resolvedBookingCode,
      }),
    })

    const rawText = await res.text()

    let data = null
    try {
      data = rawText ? JSON.parse(rawText) : null
    } catch {
      data = { message: rawText }
    }

    if (!res.ok) {
      throw new Error(
        data?.message ||
          data?.error ||
          `Validate booking failed: ${res.status}`
      )
    }

    return data
  }

  const handleValidate = async (inputCode) => {
    try {
      if (!requireAuth()) return

      const resolvedCode = inputCode?.trim()
      if (!resolvedCode) {
        toast.error('No booking code found')
        return
      }

      if (isSubmitting) return

      setIsSubmitting(true)
      setResult(null)

      const data = await validateBookingCode(resolvedCode)

      setResult({
        success: true,
        data,
      })

      toast.success(data?.message || 'Booking validated successfully')
    } catch (error) {
      console.error(error)

      setResult({
        success: false,
        data: {
          message: error.message || 'Booking validation failed',
        },
      })

      toast.error(error.message || 'Booking validation failed')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleScanResult = async (value) => {
    if (!value) return

    // chặn scan lặp liên tục cùng 1 mã
    if (value === lastScannedValue) return

    setLastScannedValue(value)
    setScannedText(value)

    const resolvedCode = extractBookingCode(value)
    setBookingCode(String(resolvedCode || ''))

    if (!resolvedCode) {
      toast.error('Cannot extract booking code from QR')
      return
    }

    await handleValidate(String(resolvedCode))
  }

  return (
    <div className='relative px-6 md:px-10 lg:px-16 py-20 min-h-screen'>
      <BlurCircle top="-100px" left="-100px" />
      <BlurCircle top="80px" left="300px" />

      <div className='max-w-5xl mx-auto'>
        <h1 className='text-2xl md:text-3xl font-semibold text-center mb-8'>
          QR Booking Validation
        </h1>

        <div className='grid lg:grid-cols-2 gap-8'>
          <div className='bg-primary/10 border border-primary/20 rounded-2xl p-6'>
            <h2 className='text-lg font-semibold mb-4'>Scan QR</h2>

            <div className='overflow-hidden rounded-xl border border-primary/20'>
              <QrReader
                constraints={{ facingMode: 'environment' }}
                onResult={(result, error) => {
                  if (result?.text) {
                    handleScanResult(result.text)
                  }

                  if (error) {
                    // không toast lỗi camera liên tục
                    console.debug(error?.message)
                  }
                }}
                containerStyle={{ width: '100%' }}
                videoStyle={{ width: '100%' }}
              />
            </div>

            <p className='text-xs text-gray-400 mt-4'>
              Scan the QR from the user ticket / booking confirmation.
            </p>
          </div>

          <div className='bg-primary/10 border border-primary/20 rounded-2xl p-6'>
            <h2 className='text-lg font-semibold mb-4'>Validation Result</h2>

            <div className='space-y-4 text-sm'>
              <div>
                <p className='text-gray-400 mb-1'>Scanned Raw Text</p>
                <div className='rounded-lg border border-primary/20 p-3 break-all min-h-[56px]'>
                  {scannedText || 'No QR scanned yet'}
                </div>
              </div>

              <div>
                <p className='text-gray-400 mb-1'>Resolved Booking Code</p>
                <input
                  value={bookingCode}
                  onChange={(e) => setBookingCode(e.target.value)}
                  placeholder='Booking code'
                  className='w-full rounded-lg border border-primary/20 bg-transparent px-3 py-2 outline-none'
                />
              </div>

              <button
                onClick={() => handleValidate(bookingCode)}
                disabled={isSubmitting}
                className='w-full py-3 rounded-full bg-primary hover:bg-primary-dull transition font-medium cursor-pointer active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed'
              >
                {isSubmitting ? 'Validating...' : 'Validate Booking'}
              </button>

              {result && (
                <div
                  className={`rounded-xl p-4 border ${
                    result.success
                      ? 'border-green-500/40 bg-green-500/10'
                      : 'border-red-500/40 bg-red-500/10'
                  }`}
                >
                  <p className='font-semibold mb-2'>
                    {result.success ? 'Validation Success' : 'Validation Failed'}
                  </p>

                  <p className='text-sm break-words'>
                    {result?.data?.message || 'No message'}
                  </p>

                  {result?.data?.bookingId && (
                    <p className='text-sm mt-2'>
                      <span className='text-gray-400'>Booking ID:</span>{' '}
                      {result.data.bookingId}
                    </p>
                  )}

                  {result?.data?.ticketId && (
                    <p className='text-sm mt-2'>
                      <span className='text-gray-400'>Ticket ID:</span>{' '}
                      {result.data.ticketId}
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default QrScanner