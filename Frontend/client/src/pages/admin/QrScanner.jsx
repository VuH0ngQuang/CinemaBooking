import React, { useCallback, useEffect, useRef, useState } from 'react'
import jsQR from 'jsqr'
import toast from 'react-hot-toast'
import BlurCircle from '../../components/BlurCircle'
import { useAuth } from '../../context/AuthContext'
import Title from '../../components/admin/Title'
import { buildApiUrl } from '../../lib/api'

const extractBookingCode = (rawValue) => {
  if (!rawValue) return ''
  if (typeof rawValue === 'string' && !rawValue.trim().startsWith('{') && !rawValue.trim().startsWith('[')) {
    return rawValue.trim()
  }
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

const validateBookingCode = async (code) => {
  const res = await fetch(buildApiUrl('/api/tickets/validate-booking'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ bookingCode: code }),
  })
  const rawText = await res.text()
  let data = null
  try {
    data = rawText ? JSON.parse(rawText) : null
  } catch {
    data = { message: rawText }
  }
  if (!res.ok) {
    throw new Error(data?.message || data?.error || `Validate booking failed: ${res.status}`)
  }
  return data
}

const STATUS_STYLES = {
  VALID:    'bg-green-500/20 text-green-400 border-green-500/30',
  USED:     'bg-gray-500/20 text-gray-400 border-gray-500/30',
  EXPIRED:  'bg-red-500/20 text-red-400 border-red-500/30',
  CANCELLED:'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
}

const TicketStatusBadge = ({ status }) => {
  const style = STATUS_STYLES[status] ?? 'bg-white/10 text-gray-400 border-white/20'
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${style}`}>
      {status ?? 'UNKNOWN'}
    </span>
  )
}

const QrScanner = () => {
  const { user, openAuthModal } = useAuth()

  const [scannedText, setScannedText] = useState('')
  const [bookingCode, setBookingCode] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const [isCameraActive, setIsCameraActive] = useState(false)
  const [cameraError, setCameraError] = useState('')
  const [cameraDevices, setCameraDevices] = useState([])
  const [selectedDeviceId, setSelectedDeviceId] = useState('')

  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const streamRef = useRef(null)
  const frameRef = useRef(null)
  // Refs to avoid stale closures in the RAF loop
  const lastScannedRef = useRef('')
  const isCameraActiveRef = useRef(false)
  const isSubmittingRef = useRef(false)

  useEffect(() => { isCameraActiveRef.current = isCameraActive }, [isCameraActive])
  useEffect(() => { isSubmittingRef.current = isSubmitting }, [isSubmitting])

  // Load camera devices once on mount
  useEffect(() => {
    navigator.mediaDevices
      .enumerateDevices()
      .then((devices) => {
        const videoInputs = devices.filter((d) => d.kind === 'videoinput')
        setCameraDevices(videoInputs)
        if (videoInputs.length > 0) setSelectedDeviceId(videoInputs[0].deviceId)
      })
      .catch(() => setCameraDevices([]))
  }, [])

  const stopCamera = useCallback(() => {
    if (frameRef.current) {
      cancelAnimationFrame(frameRef.current)
      frameRef.current = null
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop())
      streamRef.current = null
    }
    isCameraActiveRef.current = false
    setIsCameraActive(false)
  }, [])

  // Cleanup on unmount
  useEffect(() => () => stopCamera(), [stopCamera])

  const handleValidate = useCallback(async (inputCode) => {
    if (!user) {
      toast('Please login first')
      openAuthModal()
      return
    }
    const resolvedCode = inputCode?.trim()
    if (!resolvedCode) {
      toast.error('No booking code found')
      return
    }
    if (isSubmittingRef.current) return

    setIsSubmitting(true)
    isSubmittingRef.current = true
    setResult(null)

    try {
      const data = await validateBookingCode(resolvedCode)
      setResult({ success: true, data })
      toast.success(data?.message || 'Booking validated successfully')
    } catch (error) {
      setResult({ success: false, data: { message: error.message || 'Booking validation failed' } })
      toast.error(error.message || 'Booking validation failed')
    } finally {
      setIsSubmitting(false)
      isSubmittingRef.current = false
    }
  }, [user, openAuthModal])

  const scanFrame = useCallback(() => {
    const video = videoRef.current
    const canvas = canvasRef.current
    if (!video || !canvas) return

    if (video.readyState >= 2) {
      const { videoWidth: width, videoHeight: height } = video
      if (width > 0 && height > 0) {
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d', { willReadFrequently: true })
        if (ctx) {
          ctx.drawImage(video, 0, 0, width, height)
          const imageData = ctx.getImageData(0, 0, width, height)
          const code = jsQR(imageData.data, imageData.width, imageData.height)

          if (code?.data && code.data !== lastScannedRef.current) {
            lastScannedRef.current = code.data
            setScannedText(code.data)
            const resolved = extractBookingCode(code.data)
            setBookingCode(String(resolved || ''))
            if (resolved) {
              void handleValidate(String(resolved))
            } else {
              toast.error('Cannot extract booking code from QR')
            }
          }
        }
      }
    }

    if (isCameraActiveRef.current) {
      frameRef.current = requestAnimationFrame(scanFrame)
    }
  }, [handleValidate])

  useEffect(() => {
    if (isCameraActive) {
      frameRef.current = requestAnimationFrame(scanFrame)
    }
    return () => {
      if (frameRef.current) {
        cancelAnimationFrame(frameRef.current)
        frameRef.current = null
      }
    }
  }, [isCameraActive, scanFrame])

  const startCamera = useCallback(async () => {
    setCameraError('')
    const constraintsList = selectedDeviceId
      ? [{ deviceId: { exact: selectedDeviceId } }, { deviceId: { ideal: selectedDeviceId } }, true]
      : [{ facingMode: { ideal: 'environment' } }, { facingMode: 'user' }, true]

    let stream = null
    for (const videoConstraint of constraintsList) {
      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: videoConstraint, audio: false })
        break
      } catch { /* try next constraint */ }
    }

    if (!stream) {
      setCameraError('Cannot open any camera stream')
      return
    }

    streamRef.current = stream
    if (videoRef.current) {
      videoRef.current.srcObject = stream
      try {
        await videoRef.current.play()
      } catch {
        setCameraError('Cannot access camera. Please check browser permission and try again.')
        return
      }
    }
    isCameraActiveRef.current = true
    setIsCameraActive(true)
  }, [selectedDeviceId])

  return (
    <div className='relative px-6 md:px-10 lg:px-16 py-20 min-h-screen'>
      <BlurCircle top="-100px" left="-100px" />
      <BlurCircle top="80px" left="300px" />

      <div className='max-w-5xl mx-auto'>
        <Title text1='QR' text2='Booking Validation' />

        <div className='grid lg:grid-cols-2 gap-8 mt-8'>
          <div className='bg-primary/10 border border-primary/20 rounded-2xl p-6'>
            <h2 className='text-lg font-semibold mb-4'>Scan QR</h2>

            <div className='space-y-3'>
              <div className='overflow-hidden rounded-xl border border-primary/20 bg-black/40 aspect-video'>
                <video ref={videoRef} className='w-full h-full object-cover' muted playsInline autoPlay />
                <canvas ref={canvasRef} className='hidden' />
              </div>

              {cameraDevices.length > 0 && (
                <div>
                  <label className='block text-xs text-gray-400 mb-1'>Camera device</label>
                  <select
                    className='w-full rounded-md bg-black/30 border border-white/15 px-2.5 py-2 text-sm'
                    value={selectedDeviceId}
                    onChange={(e) => setSelectedDeviceId(e.target.value)}
                    disabled={isCameraActive}
                  >
                    {cameraDevices.map((device, index) => (
                      <option key={device.deviceId} value={device.deviceId}>
                        {device.label || `Camera ${index + 1}`}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div className='flex gap-2'>
                <button
                  onClick={startCamera}
                  disabled={isCameraActive}
                  className='px-4 py-2 rounded-lg bg-primary text-black font-medium disabled:opacity-50 disabled:cursor-not-allowed'
                >
                  Start Camera
                </button>
                <button
                  onClick={stopCamera}
                  disabled={!isCameraActive}
                  className='px-4 py-2 rounded-lg border border-primary/20 disabled:opacity-50 disabled:cursor-not-allowed'
                >
                  Stop Camera
                </button>
              </div>

              {cameraError && <p className='text-sm text-red-400'>{cameraError}</p>}
            </div>

            <p className='text-xs text-gray-400 mt-4'>
              Scan the QR from the user ticket / booking confirmation.
            </p>
          </div>

          <div className='bg-primary/10 border border-primary/20 rounded-2xl p-6 flex flex-col gap-4'>
            <h2 className='text-lg font-semibold'>Validation Result</h2>

            <div>
              <p className='text-xs text-gray-400 mb-1'>Scanned Raw Text</p>
              <div className='rounded-lg border border-primary/20 p-3 break-all min-h-[44px] text-sm'>
                {scannedText || <span className='text-gray-500'>No QR scanned yet</span>}
              </div>
            </div>

            <div>
              <p className='text-xs text-gray-400 mb-1'>Booking Code</p>
              <input
                value={bookingCode}
                onChange={(e) => setBookingCode(e.target.value)}
                placeholder='Booking code'
                className='w-full rounded-lg border border-primary/20 bg-transparent px-3 py-2 outline-none text-sm'
              />
            </div>

            <button
              onClick={() => handleValidate(bookingCode)}
              disabled={isSubmitting}
              className='w-full py-2.5 rounded-full bg-primary hover:bg-primary-dull transition font-medium text-sm cursor-pointer active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed'
            >
              {isSubmitting ? 'Validating...' : 'Validate Booking'}
            </button>

            {result && (
              <div className={`rounded-xl border text-sm ${
                result.success ? 'border-green-500/40 bg-green-500/10' : 'border-red-500/40 bg-red-500/10'
              }`}>
                <div className='px-4 py-3 border-b border-white/10 flex items-center gap-2'>
                  <span className={`inline-block w-2 h-2 rounded-full ${result.success ? 'bg-green-400' : 'bg-red-400'}`} />
                  <span className='font-semibold'>
                    {result.success ? 'Booking Valid' : 'Validation Failed'}
                  </span>
                </div>

                <div className='px-4 py-3 space-y-1 text-gray-300'>
                  {result.data?.message && <p>{result.data.message}</p>}
                  {result.data?.bookingCode && (
                    <p><span className='text-gray-500'>Booking Code: </span>{result.data.bookingCode}</p>
                  )}
                </div>

                {result.success && result.data?.tickets?.length > 0 && (
                  <div className='px-4 pb-4 space-y-3'>
                    <p className='text-xs text-gray-400 font-medium uppercase tracking-wide'>
                      Tickets ({result.data.tickets.length})
                    </p>
                    {result.data.tickets.map((ticket) => (
                      <div
                        key={ticket.ticketId}
                        className='rounded-lg border border-white/10 bg-black/30 p-3 space-y-1.5'
                      >
                        <div className='flex items-center justify-between'>
                          <span className='font-medium'>Seat {ticket.seatNumber ?? ticket.seatId}</span>
                          <TicketStatusBadge status={ticket.status} />
                        </div>
                        <div className='text-xs text-gray-400 space-y-0.5'>
                          <p><span className='text-gray-500'>Ticket ID: </span>{ticket.ticketId}</p>
                          <p><span className='text-gray-500'>Code: </span>{ticket.ticketCode}</p>
                          {ticket.issuedAt && (
                            <p><span className='text-gray-500'>Issued: </span>{new Date(ticket.issuedAt).toLocaleString()}</p>
                          )}
                          {ticket.usedAt && (
                            <p><span className='text-gray-500'>Used: </span>{new Date(ticket.usedAt).toLocaleString()}</p>
                          )}
                          {ticket.validUntil && (
                            <p><span className='text-gray-500'>Valid until: </span>{new Date(ticket.validUntil).toLocaleString()}</p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default QrScanner
