import React, { useEffect, useRef, useState } from 'react'
import jsQR from 'jsqr'
import toast from 'react-hot-toast'
import BlurCircle from '../../components/BlurCircle'
import { useAuth } from '../../context/AuthContext'
import Title from '../../components/admin/Title'
import { validateTicketByCode } from '../../lib/ticketApi'
import { useAuth } from '../../context/AuthContext'

const ticketFields = [
  ['ticketId', 'ticket_id'],
  ['ticketCode', 'ticket_code'],
  ['bookingId', 'booking_id'],
  ['seatId', 'seat_id'],
  ['seatNumber', 'seat_number'],
  ['issuedAt', 'issued_at'],
  ['validUntil', 'valid_until'],
  ['usedAt', 'used_at'],
  ['status', 'status'],
]

const getDisplayValue = (value) => {
  if (value === null || value === undefined || value === '') return 'N/A'
  return String(value)
}

const QrScanner = () => {
  const baseUrl = import.meta.env.VITE_BASE_URL?.replace(/\/$/, '')
  const { token, openAuthModal } = useAuth()

  const [scannedText, setScannedText] = useState('')
  const [bookingCode, setBookingCode] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const [lastScannedValue, setLastScannedValue] = useState('')
  const [isCameraActive, setIsCameraActive] = useState(false)
  const [cameraError, setCameraError] = useState('')

  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const streamRef = useRef(null)
  const frameRef = useRef(null)
  const lastValidatedRef = useRef('')

  const { token } = useAuth()

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
  const [isScanning, setIsScanning] = useState(false)
  const [scanResult, setScanResult] = useState('')
  const [ticketData, setTicketData] = useState(null)
  const [validationMessage, setValidationMessage] = useState('')
  const [validationSuccess, setValidationSuccess] = useState(null)
  const [error, setError] = useState('')
  const [decoderName, setDecoderName] = useState('BarcodeDetector')
  const [cameraStatus, setCameraStatus] = useState('Idle')
  const [cameraDevices, setCameraDevices] = useState([])
  const [selectedDeviceId, setSelectedDeviceId] = useState('')
  const [isValidating, setIsValidating] = useState(false)

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

  const extractTicketCode = (rawValue) => {
    const parsed = parseTicketData(rawValue)

    if (parsed?.ticket_code) return parsed.ticket_code
    if (parsed?.ticketCode) return parsed.ticketCode

    return rawValue?.trim() || ''
  }

  const stopScan = () => {
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

  const handleValidation = async (rawValue) => {
    const ticketCode = extractTicketCode(rawValue)

    if (!ticketCode) {
      setError('QR code does not contain a valid ticket code.')
      setValidationMessage('')
      setValidationSuccess(false)
      setTicketData(null)
      return
    }

    if (lastValidatedRef.current === ticketCode) {
      return
    }

    lastValidatedRef.current = ticketCode
    setIsValidating(true)
    setError('')
    setValidationMessage('Validating ticket...')
    setValidationSuccess(null)

    try {
      const response = await validateTicketByCode(ticketCode, token)

      setValidationMessage(response.message || 'Validation completed.')
      setValidationSuccess(Boolean(response.success))
      setTicketData(response.ticket || null)

      if (!response.success && !response.ticket) {
        setError(response.message || 'Ticket is invalid.')
      }
    } catch (validationError) {
      lastValidatedRef.current = ''
      setValidationMessage('')
      setValidationSuccess(false)
      setTicketData(null)
      setError(validationError.message || 'Unable to validate ticket.')
    } finally {
      setIsValidating(false)
    }
  }

  const scanFrame = async () => {
    if (!videoRef.current) return

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

      if (rawValue) {
        setScanResult(rawValue)
        await handleValidation(rawValue)
      }
    } catch {
      setError('Unable to decode QR in current frame.')
    } finally {
      if (isScanning) {
        frameRef.current = requestAnimationFrame(scanFrame)
      }
    }
  }

  const startScan = async () => {
    setError('')
    setScanResult('')
    setTicketData(null)
    setValidationMessage('')
    setValidationSuccess(null)
    detectorRef.current = null
    lastValidatedRef.current = ''
    setDecoderName('jsQR')
    setCameraStatus('Requesting camera access...')

  const startCamera = async () => {
    setCameraError('')

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' } },
        audio: false,
      })
      const videoConstraintsList = selectedDeviceId
        ? [{ deviceId: { exact: selectedDeviceId } }, { deviceId: { ideal: selectedDeviceId } }, true]
        : [{ facingMode: { ideal: 'environment' } }, { facingMode: 'user' }, true]

      let stream = null
      for (const videoConstraint of videoConstraintsList) {
        try {
          stream = await navigator.mediaDevices.getUserMedia({
            video: videoConstraint,
            audio: false,
          })
          break
        } catch {
          // Try the next fallback constraint.
        }
      }

      if (!stream) {
        throw new Error('Cannot open any camera stream')
      }

      streamRef.current = stream
      setIsCameraActive(true)

      if (videoRef.current) {
        videoRef.current.srcObject = stream
        await videoRef.current.play()
      }

      setIsScanning(true)
      setCameraStatus('Camera is live. Point to a QR code.')
    } catch {
      setError('Cannot access camera. Please check browser permission and try again.')
      setCameraStatus('Camera is unavailable.')
    }
  }

  useEffect(() => {
    loadCameraDevices()
  }, [])

  useEffect(() => {
    if (isScanning) {
      frameRef.current = requestAnimationFrame(scanFrame)
    } catch {
      setCameraError('Cannot access camera. Please grant camera permission and try again.')
      setIsCameraActive(false)
    }
  }

  useEffect(() => {
    return () => {
      stopCamera()
    }
  }, [])

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

            <div className='space-y-3'>
              <div className='overflow-hidden rounded-xl border border-primary/20 bg-black/40 aspect-video'>
                <video
                  ref={videoRef}
                  className='w-full h-full object-cover'
                  muted
                  playsInline
                  autoPlay
                />
                <canvas ref={canvasRef} className='hidden' />
              </div>

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

              {cameraError && (
                <p className='text-sm text-red-400'>{cameraError}</p>
              )}
            </div>

            <p className='text-xs text-gray-400 mt-4'>
              Scan the QR from the user ticket / booking confirmation.
            </p>
              )}
            </div>
          </div>

          <div className='mb-3'>
            <label className='block text-xs text-gray-400 mb-1'>Camera device</label>
            <select
              className='w-full rounded-md bg-black/30 border border-white/15 px-2.5 py-2 text-sm'
              value={selectedDeviceId}
              onChange={(event) => setSelectedDeviceId(event.target.value)}
              disabled={isScanning}
            >
              {cameraDevices.length === 0 ? (
                <option value=''>No camera device found</option>
              ) : (
                cameraDevices.map((device, index) => (
                  <option key={device.deviceId} value={device.deviceId}>
                    {device.label || `Camera ${index + 1}`}
                  </option>
                ))
              )}
            </select>
          </div>

          <p className='text-xs text-gray-400 mb-3'>{cameraStatus}</p>

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

          {validationMessage && (
            <div
              className={`mb-3 rounded-md border p-3 text-sm ${
                validationSuccess === true
                  ? 'border-green-500/30 bg-green-500/10 text-green-300'
                  : validationSuccess === false
                  ? 'border-red-500/30 bg-red-500/10 text-red-300'
                  : 'border-white/10 bg-black/30 text-gray-300'
              }`}
            >
              {validationMessage}
            </div>
          )}

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
          {ticketData ? (
            <div className='rounded-md border border-white/10 bg-black/30 p-3 text-sm space-y-2'>
              {ticketFields.map(([fieldKey, fieldLabel]) => (
                <div
                  key={fieldKey}
                  className='flex items-start justify-between gap-3 border-b border-white/10 pb-2 last:border-b-0 last:pb-0'
                >
                  <span className='text-gray-400'>{fieldLabel}</span>
                  <span className='text-right break-all'>{getDisplayValue(ticketData[fieldKey])}</span>
                </div>
              )}
            </div>
          ) : (
            <div className='min-h-40 rounded-md border border-white/10 bg-black/30 p-3 text-sm break-all whitespace-pre-wrap'>
              {scanResult || 'No QR code detected yet.'}
            </div>
          )}

          {isValidating && (
            <p className='text-xs text-gray-400 mt-3'>Checking ticket with backend...</p>
          )}

          {error && <p className='text-sm text-red-400 mt-3'>{error}</p>}

          <p className='text-xs text-gray-400 mt-3'>
            Live flow: scan QR, extract ticket code, validate via backend API.
          </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default QrScanner