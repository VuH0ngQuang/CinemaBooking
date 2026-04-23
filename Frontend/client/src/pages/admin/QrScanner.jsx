import React, { useEffect, useRef, useState } from 'react'
import jsQR from 'jsqr'
import toast from 'react-hot-toast'
import BlurCircle from '../../components/BlurCircle'
import { useAuth } from '../../context/AuthContext'
import Title from '../../components/admin/Title'
import { buildApiUrl } from '../../lib/api'

const QrScanner = () => {
  const { user, openAuthModal } = useAuth()

  const [scannedText, setScannedText] = useState('')
  const [bookingCode, setBookingCode] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const [lastScannedValue, setLastScannedValue] = useState('')
  const [isCameraActive, setIsCameraActive] = useState(false)
  const [cameraError, setCameraError] = useState('')
  const [cameraDevices, setCameraDevices] = useState([])
  const [selectedDeviceId, setSelectedDeviceId] = useState('')

  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const streamRef = useRef(null)
  const frameRef = useRef(null)

  const requireAuth = () => {
    if (user) return true
    toast('Please login first')
    openAuthModal()
    return false
  }

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

  const validateBookingCode = async (resolvedBookingCode) => {
    const res = await fetch(buildApiUrl('/api/tickets/validate-booking'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ bookingCode: resolvedBookingCode }),
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
      setResult({ success: true, data })
      toast.success(data?.message || 'Booking validated successfully')
    } catch (error) {
      console.error(error)
      setResult({ success: false, data: { message: error.message || 'Booking validation failed' } })
      toast.error(error.message || 'Booking validation failed')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleScanResult = async (value) => {
    if (!value || value === lastScannedValue) return

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

  const scanFrame = () => {
    const video = videoRef.current
    const canvas = canvasRef.current

    if (!video || !canvas) return

    if (video.readyState >= 2) {
      const width = video.videoWidth
      const height = video.videoHeight

      if (width > 0 && height > 0) {
        canvas.width = width
        canvas.height = height
        const context = canvas.getContext('2d', { willReadFrequently: true })

        if (context) {
          context.drawImage(video, 0, 0, width, height)
          const imageData = context.getImageData(0, 0, width, height)
          const code = jsQR(imageData.data, imageData.width, imageData.height)

          if (code?.data) {
            void handleScanResult(code.data)
          }
        }
      }
    }

    if (isCameraActive) {
      frameRef.current = requestAnimationFrame(scanFrame)
    }
  }

  const stopCamera = () => {
    if (frameRef.current) {
      cancelAnimationFrame(frameRef.current)
      frameRef.current = null
    }

    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop())
      streamRef.current = null
    }

    setIsCameraActive(false)
  }

  const startCamera = async () => {
    setCameraError('')

    const videoConstraintsList = selectedDeviceId
      ? [{ deviceId: { exact: selectedDeviceId } }, { deviceId: { ideal: selectedDeviceId } }, true]
      : [{ facingMode: { ideal: 'environment' } }, { facingMode: 'user' }, true]

    let stream = null
    for (const videoConstraint of videoConstraintsList) {
      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: videoConstraint, audio: false })
        break
      } catch {
        // try next constraint
      }
    }

    if (!stream) {
      setCameraError('Cannot open any camera stream')
      return
    }

    try {
      streamRef.current = stream

      if (videoRef.current) {
        videoRef.current.srcObject = stream
        await videoRef.current.play()
      }

      setIsCameraActive(true)
    } catch {
      setCameraError('Cannot access camera. Please check browser permission and try again.')
    }
  }

  const loadCameraDevices = async () => {
    try {
      const devices = await navigator.mediaDevices.enumerateDevices()
      const videoInputs = devices.filter((device) => device.kind === 'videoinput')
      setCameraDevices(videoInputs)
      if (!selectedDeviceId && videoInputs.length > 0) {
        setSelectedDeviceId(videoInputs[0].deviceId)
      }
    } catch {
      setCameraDevices([])
    }
  }

  useEffect(() => {
    loadCameraDevices()
  }, [])

  useEffect(() => {
    if (isCameraActive) {
      frameRef.current = requestAnimationFrame(scanFrame)
    }
    return () => {
      if (frameRef.current) {
        cancelAnimationFrame(frameRef.current)
      }
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isCameraActive])

  useEffect(() => {
    return () => {
      stopCamera()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

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
                      <span className='text-gray-400'>Booking ID: </span>
                      {result.data.bookingId}
                    </p>
                  )}

                  {result?.data?.ticketId && (
                    <p className='text-sm mt-2'>
                      <span className='text-gray-400'>Ticket ID: </span>
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
