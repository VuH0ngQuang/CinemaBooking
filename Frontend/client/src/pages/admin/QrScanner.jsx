import React, { useEffect, useRef, useState } from 'react'
import { CameraIcon, CameraOffIcon, QrCodeIcon } from 'lucide-react'
import jsQR from 'jsqr'
import Title from '../../components/admin/Title'

const ticketFields = [
  'ticket_id',
  'ticket_code',
  'booking_id',
  'seat_id',
  'issued_at',
  'valid_until',
  'used_at',
  'status',
]

const getDisplayValue = (value) => {
  if (value === null || value === undefined || value === '') return 'N/A'
  return String(value)
}

const QrScanner = () => {
  const videoRef = useRef(null)
  const streamRef = useRef(null)
  const detectorRef = useRef(null)
  const canvasRef = useRef(null)
  const frameRef = useRef(null)

  const [isScanning, setIsScanning] = useState(false)
  const [scanResult, setScanResult] = useState('')
  const [ticketData, setTicketData] = useState(null)
  const [error, setError] = useState('')
  const [decoderName, setDecoderName] = useState('BarcodeDetector')
  const [cameraStatus, setCameraStatus] = useState('Idle')
  const [cameraDevices, setCameraDevices] = useState([])
  const [selectedDeviceId, setSelectedDeviceId] = useState('')

  const parseTicketData = (rawValue) => {
    try {
      const parsed = JSON.parse(rawValue)
      if (typeof parsed !== 'object' || parsed === null) return null
      return parsed
    } catch {
      return null
    }
  }

  const stopScan = () => {
    if (frameRef.current) {
      cancelAnimationFrame(frameRef.current)
      frameRef.current = null
    }

    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop())
      streamRef.current = null
    }

    setIsScanning(false)
    setCameraStatus('Camera stopped.')
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

  const scanFrame = async () => {
    if (!videoRef.current) return

    const video = videoRef.current
    if (video.readyState < 2) {
      if (isScanning) frameRef.current = requestAnimationFrame(scanFrame)
      return
    }

    try {
      let rawValue = ''

      if (detectorRef.current) {
        const barcodes = await detectorRef.current.detect(video)
        rawValue = barcodes[0]?.rawValue || ''
      } else if (canvasRef.current) {
        const canvas = canvasRef.current
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        const context = canvas.getContext('2d', { willReadFrequently: true })
        if (context) {
          context.drawImage(video, 0, 0, canvas.width, canvas.height)
          const imageData = context.getImageData(0, 0, canvas.width, canvas.height)
          const code = jsQR(imageData.data, imageData.width, imageData.height)
          rawValue = code?.data || ''
        }
      }

      if (rawValue) {
        setScanResult(rawValue)
        setTicketData(parseTicketData(rawValue))
      }
    } catch (scanError) {
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
    detectorRef.current = null
    setDecoderName('jsQR')
    setCameraStatus('Requesting camera access...')

    try {
      if ('BarcodeDetector' in window) {
        detectorRef.current = new window.BarcodeDetector({ formats: ['qr_code'] })
        setDecoderName('BarcodeDetector')
      }

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
        } catch (streamError) {
          // Try the next fallback constraint.
        }
      }

      if (!stream) {
        throw new Error('Cannot open any camera stream')
      }

      streamRef.current = stream

      if (videoRef.current) {
        videoRef.current.srcObject = stream
        videoRef.current.autoplay = true
        videoRef.current.muted = true
        videoRef.current.setAttribute('muted', '')
        videoRef.current.setAttribute('autoplay', '')
        await new Promise((resolve) => {
          videoRef.current.onloadedmetadata = resolve
        })
        await videoRef.current.play()
      }

      setIsScanning(true)
      setCameraStatus('Camera is live. Point to a QR code.')
    } catch (cameraError) {
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
    }

    return () => {
      if (frameRef.current) {
        cancelAnimationFrame(frameRef.current)
        frameRef.current = null
      }
    }
  }, [isScanning])

  useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop())
        streamRef.current = null
      }
    }
  }, [])

  return (
    <div className='space-y-6'>
      <Title text1='QR Code' text2='Scanner' />

      <div className='grid grid-cols-1 xl:grid-cols-3 gap-4'>
        <div className='xl:col-span-2 rounded-xl border border-primary/20 bg-primary/10 p-4'>
          <div className='flex items-center justify-between gap-3 flex-wrap mb-4'>
            <p className='text-sm text-gray-300'>
              Use your computer camera to scan ticket QR code ({decoderName} decoder).
            </p>
            <div className='flex items-center gap-2'>
              {!isScanning ? (
                <button
                  onClick={startScan}
                  className='cursor-pointer inline-flex items-center gap-2 px-3 py-2 rounded-md bg-primary text-black font-medium hover:opacity-90 transition-opacity duration-200'
                >
                  <CameraIcon className='w-4 h-4' />
                  Start camera
                </button>
              ) : (
                <button
                  onClick={stopScan}
                  className='cursor-pointer inline-flex items-center gap-2 px-3 py-2 rounded-md bg-white/15 border border-white/20 hover:bg-white/25 transition-colors duration-200'
                >
                  <CameraOffIcon className='w-4 h-4' />
                  Stop camera
                </button>
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

          <div className='rounded-lg overflow-hidden border border-white/10 bg-black/40 aspect-video'>
            <video ref={videoRef} className='w-full h-full object-cover' muted autoPlay playsInline />
            <canvas ref={canvasRef} className='hidden' />
          </div>
        </div>

        <div className='rounded-xl border border-primary/20 bg-primary/10 p-4'>
          <div className='flex items-center gap-2 mb-3'>
            <QrCodeIcon className='w-5 h-5' />
            <h2 className='font-medium'>Ticket summary</h2>
          </div>

          {ticketData ? (
            <div className='rounded-md border border-white/10 bg-black/30 p-3 text-sm space-y-2'>
              {ticketFields.map((field) => (
                <div key={field} className='flex items-start justify-between gap-3 border-b border-white/10 pb-2 last:border-b-0 last:pb-0'>
                  <span className='text-gray-400'>{field}</span>
                  <span className='text-right break-all'>{getDisplayValue(ticketData[field])}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className='min-h-40 rounded-md border border-white/10 bg-black/30 p-3 text-sm break-all whitespace-pre-wrap'>
              {scanResult || 'No QR code detected yet.'}
            </div>
          )}

          <p className='text-xs text-gray-400 mt-3'>
            Expected QR payload (JSON): ticket fields from `Tickets.java`.
          </p>

          {error && <p className='text-sm text-red-400 mt-3'>{error}</p>}

          <p className='text-xs text-gray-400 mt-3'>
            Static flow only: scan and display ticket data for staff check-in, no API call yet.
          </p>
        </div>
      </div>
    </div>
  )
}

export default QrScanner
