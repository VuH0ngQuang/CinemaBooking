import React, { useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import Title from '../../components/admin/Title'
import { useAuth } from '../../context/AuthContext'
import {
  getAllBookingSeats,
  getAllCinemas,
  getAllScreeningRooms,
  getAllTickets,
  getAllUsers,
} from '../../lib/adminManagementApi'

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') return 'N/A'

  if (typeof value === 'boolean') {
    return value ? 'true' : 'false'
  }

  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return 'N/A'
    }
  }

  return String(value)
}

const formatDateTime = (value) => {
  if (!value) return 'N/A'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)

  return date.toLocaleString()
}

const pageConfigMap = {
  '/admin/users': {
    title1: 'Manage',
    title2: 'Users',
    fetcherKey: 'users',
    columns: [
      { key: 'user_id', label: 'User ID' },
      { key: 'full_name', label: 'Full Name' },
      { key: 'email', label: 'Email' },
      { key: 'role', label: 'Role' },
      { key: 'status', label: 'Status' },
      { key: 'created_at', label: 'Created At', isDate: true },
    ],
  },
  '/admin/cinemas': {
    title1: 'Manage',
    title2: 'Cinemas',
    fetcherKey: 'cinemas',
    columns: [
      { key: 'cinema_id', label: 'Cinema ID' },
      { key: 'name', label: 'Name' },
      { key: 'address', label: 'Address' },
      { key: 'created_at', label: 'Created At', isDate: true },
      { key: 'updated_at', label: 'Updated At', isDate: true },
    ],
  },
  '/admin/screening-rooms': {
    title1: 'Manage',
    title2: 'Screening Rooms',
    fetcherKey: 'screeningRooms',
    columns: [
      { key: 'screening_room_id', label: 'Room ID' },
      { key: 'room_name', label: 'Room Name' },
      { key: 'amount_rows', label: 'Rows' },
      { key: 'amount_cols', label: 'Cols' },
      { key: 'cinema_id', label: 'Cinema ID' },
      { key: 'created_at', label: 'Created At', isDate: true },
    ],
  },
  '/admin/booking-seats': {
    title1: 'Manage',
    title2: 'Booking Seats',
    fetcherKey: 'bookingSeats',
    columns: [
      { key: 'booking_seat_id', label: 'Booking Seat ID' },
      { key: 'booking_id', label: 'Booking ID' },
      { key: 'seat_id', label: 'Seat ID' },
      { key: 'price', label: 'Price' },
      { key: 'created_at', label: 'Created At', isDate: true },
    ],
  },
  '/admin/tickets': {
    title1: 'Manage',
    title2: 'Tickets',
    fetcherKey: 'tickets',
    columns: [
      { key: 'ticket_id', label: 'Ticket ID' },
      { key: 'ticket_code', label: 'Ticket Code' },
      { key: 'booking_id', label: 'Booking ID' },
      { key: 'seat_id', label: 'Seat ID' },
      { key: 'seat_number', label: 'Seat Number' },
      { key: 'issued_at', label: 'Issued At', isDate: true },
      { key: 'valid_until', label: 'Valid Until', isDate: true },
      { key: 'used_at', label: 'Used At', isDate: true },
      { key: 'status', label: 'Status' },
    ],
  },
}

const normalizeRow = (row) => {
  return {
    ...row,
    user_id: row.user_id ?? row.userId,
    full_name: row.full_name ?? row.fullName,
    created_at: row.created_at ?? row.createdAt,
    updated_at: row.updated_at ?? row.updatedAt,

    cinema_id: row.cinema_id ?? row.cinemaId,

    screening_room_id: row.screening_room_id ?? row.screeningRoomId ?? row.room_id ?? row.roomId,
    room_name: row.room_name ?? row.roomName,

    booking_seat_id: row.booking_seat_id ?? row.bookingSeatId,
    booking_id: row.booking_id ?? row.bookingId,
    seat_id: row.seat_id ?? row.seatId,

    ticket_id: row.ticket_id ?? row.ticketId,
    ticket_code: row.ticket_code ?? row.ticketCode,
    seat_number: row.seat_number ?? row.seatNumber,
    issued_at: row.issued_at ?? row.issuedAt,
    valid_until: row.valid_until ?? row.validUntil,
    used_at: row.used_at ?? row.usedAt,
  }
}

const getRowKey = (row, index) => {
  return (
    row.user_id ??
    row.cinema_id ??
    row.screening_room_id ??
    row.booking_seat_id ??
    row.ticket_id ??
    row.id ??
    index
  )
}

const ManagementPages = () => {
  const location = useLocation()
  const { token } = useAuth()

  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const config = pageConfigMap[location.pathname]

  const fetcherMap = useMemo(() => {
    return {
      users: getAllUsers,
      cinemas: getAllCinemas,
      screeningRooms: getAllScreeningRooms,
      bookingSeats: getAllBookingSeats,
      tickets: getAllTickets,
    }
  }, [])

  useEffect(() => {
    const fetchData = async () => {
      if (!config) {
        setRows([])
        setError('Unsupported admin page.')
        setLoading(false)
        return
      }

      try {
        setLoading(true)
        setError('')

        const fetcher = fetcherMap[config.fetcherKey]
        const data = await fetcher(token)

        const normalizedRows = Array.isArray(data) ? data.map(normalizeRow) : []
        setRows(normalizedRows)
      } catch (err) {
        setError(err.message || 'Failed to load data.')
        setRows([])
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [config, fetcherMap, token])

  if (!config) {
    return (
      <div className='space-y-6'>
        <Title text1='Admin' text2='Management' />
        <div className='rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300'>
          Unsupported admin page.
        </div>
      </div>
    )
  }

  return (
    <div className='space-y-6'>
      <Title text1={config.title1} text2={config.title2} />

      {loading && (
        <div className='rounded-xl border border-primary/20 bg-primary/10 p-4 text-sm text-gray-300'>
          Loading data...
        </div>
      )}

      {error && (
        <div className='rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300'>
          {error}
        </div>
      )}

      {!loading && !error && (
        <div className='rounded-xl border border-primary/20 bg-primary/10 overflow-hidden'>
          <div className='overflow-x-auto'>
            <table className='w-full text-sm'>
              <thead className='bg-black/30 text-left'>
                <tr>
                  {config.columns.map((column) => (
                    <th key={column.key} className='px-4 py-3'>
                      {column.label}
                    </th>
                  ))}
                </tr>
              </thead>

              <tbody>
                {rows.length === 0 ? (
                  <tr>
                    <td
                      colSpan={config.columns.length}
                      className='px-4 py-6 text-center text-gray-400'
                    >
                      No data found.
                    </td>
                  </tr>
                ) : (
                  rows.map((row, index) => (
                    <tr
                      key={getRowKey(row, index)}
                      className='border-t border-white/10 hover:bg-white/5 transition-colors'
                    >
                      {config.columns.map((column) => {
                        const rawValue = row[column.key]

                        return (
                          <td key={column.key} className='px-4 py-3 break-all'>
                            {column.isDate ? formatDateTime(rawValue) : formatValue(rawValue)}
                          </td>
                        )
                      })}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

export default ManagementPages