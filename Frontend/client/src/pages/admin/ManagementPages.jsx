import React from 'react'
import Title from '../../components/admin/Title'
import { DatabaseIcon, PencilIcon, PlusIcon, RotateCcwIcon, Trash2Icon } from 'lucide-react'

const mockManagementData = {
  users: {
    endpoint: '/api/users',
    operations: ['list', 'get', 'update', 'delete', 'restore'],
    columns: ['user_id', 'email', 'full_name', 'role', 'status', 'is_deleted', 'created_at', 'updated_at'],
    rows: [
      {
        user_id: 1000000001,
        email: 'an.nguyen@cinema.vn',
        full_name: 'Nguyen Van An',
        role: 'ADMIN',
        status: 'ACTIVE',
        is_deleted: false,
        created_at: '2026-03-21T08:10:00Z',
        updated_at: '2026-04-10T14:40:00Z',
      },
      {
        user_id: 1000000002,
        email: 'khoa.tran@cinema.vn',
        full_name: 'Tran Minh Khoa',
        role: 'CUSTOMER',
        status: 'ACTIVE',
        is_deleted: false,
        created_at: '2026-03-25T11:30:00Z',
        updated_at: '2026-04-11T10:00:00Z',
      },
      {
        user_id: 1000000003,
        email: 'ha.le@cinema.vn',
        full_name: 'Le Thu Ha',
        role: 'CUSTOMER',
        status: 'INACTIVE',
        is_deleted: true,
        created_at: '2026-03-27T09:20:00Z',
        updated_at: '2026-04-09T16:15:00Z',
      },
    ],
  },
  cinemas: {
    endpoint: '/api/cinemas',
    operations: ['create', 'read', 'update', 'delete'],
    columns: ['cinemas_id', 'name', 'address', 'created_at'],
    rows: [
      {
        cinemas_id: 200000001,
        name: 'Galaxy Riverside',
        address: '12 Riverside Street, District 7, Ho Chi Minh City',
        created_at: '2026-01-08T08:00:00Z',
      },
      {
        cinemas_id: 200000002,
        name: 'Lotte Landmark',
        address: '54 Kim Ma, Ba Dinh, Hanoi',
        created_at: '2026-01-10T08:00:00Z',
      },
      {
        cinemas_id: 200000003,
        name: 'CGV Beachfront',
        address: '8 Vo Nguyen Giap, Son Tra, Da Nang',
        created_at: '2026-01-12T08:00:00Z',
      },
    ],
  },
  screeningRooms: {
    endpoint: '/api/screeningrooms',
    operations: ['create', 'read', 'update', 'delete'],
    columns: ['room_id', 'cinemas_id', 'room_name', 'amount_rows', 'amount_cols'],
    rows: [
      { room_id: 300000101, cinemas_id: 200000001, room_name: 'Room A', amount_rows: 10, amount_cols: 12 },
      { room_id: 300000202, cinemas_id: 200000002, room_name: 'Room B', amount_rows: 9, amount_cols: 10 },
      { room_id: 300000303, cinemas_id: 200000003, room_name: 'Room C', amount_rows: 8, amount_cols: 8 },
    ],
  },
  bookingSeats: {
    endpoint: '/api/booking-seats',
    operations: ['create', 'read', 'update', 'delete'],
    columns: ['booking_seat_id', 'booking_id', 'seat_id', 'price', 'status'],
    rows: [
      { booking_seat_id: 500000001, booking_id: 400000001, seat_id: 700000101, price: 95000, status: 'LOCKED' },
      { booking_seat_id: 500000002, booking_id: 400000001, seat_id: 700000102, price: 95000, status: 'BOOKED' },
      { booking_seat_id: 500000003, booking_id: 400000002, seat_id: 700000208, price: 125000, status: 'BOOKED' },
    ],
  },
  tickets: {
    endpoint: '/api/tickets',
    operations: ['generate', 'validate', 'validate-booking', 'get'],
    columns: ['ticket_id', 'ticket_code', 'booking_id', 'seat_id', 'issued_at', 'valid_until', 'used_at', 'status'],
    rows: [
      {
        ticket_id: 600000001,
        ticket_code: 'TCK-9AF3',
        booking_id: 400000001,
        seat_id: 700000101,
        issued_at: '2026-04-13T17:40:00Z',
        valid_until: '2026-04-13T21:30:00Z',
        used_at: null,
        status: 'VALID',
      },
      {
        ticket_id: 600000002,
        ticket_code: 'TCK-6ZS8',
        booking_id: 400000002,
        seat_id: 700000208,
        issued_at: '2026-04-13T18:10:00Z',
        valid_until: '2026-04-13T22:00:00Z',
        used_at: null,
        status: 'VALID',
      },
      {
        ticket_id: 600000003,
        ticket_code: 'TCK-2QX1',
        booking_id: 400000003,
        seat_id: 700000305,
        issued_at: '2026-04-12T11:00:00Z',
        valid_until: '2026-04-12T15:00:00Z',
        used_at: '2026-04-12T13:22:00Z',
        status: 'USED',
      },
    ],
  },
}

const renderRowValues = (row) => Object.keys(row).map((key) => row[key])

const ManagementPage = ({ title1, title2, sectionKey }) => {
  const section = mockManagementData[sectionKey]

  return (
    <div className='space-y-6'>
      <Title text1={title1} text2={title2} />

      <div className='grid grid-cols-1 lg:grid-cols-3 gap-4'>
        <div className='lg:col-span-2 bg-primary/10 border border-primary/20 rounded-xl p-4'>
          <div className='flex items-center justify-between gap-3 flex-wrap'>
            <div>
              <p className='text-sm text-gray-300'>Endpoint</p>
              <p className='font-medium'>{section.endpoint}</p>
            </div>
            <button className='cursor-pointer inline-flex items-center gap-2 px-3 py-2 rounded-md bg-primary text-black font-medium hover:opacity-90 transition-opacity duration-200'>
              <PlusIcon className='w-4 h-4' />
              Add mock item
            </button>
          </div>
        </div>

        <div className='bg-primary/10 border border-primary/20 rounded-xl p-4'>
          <p className='text-sm text-gray-300 mb-2'>Supported operations</p>
          <div className='flex flex-wrap gap-2'>
            {section.operations.map((operation) => (
              <span
                key={operation}
                className='inline-flex items-center px-2.5 py-1 rounded-md text-xs bg-black/40 border border-white/20'
              >
                {operation}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div className='rounded-xl border border-primary/20 overflow-hidden'>
        <div className='overflow-x-auto'>
          <table className='w-full text-sm'>
            <thead className='bg-primary/15'>
              <tr>
                {section.columns.map((column) => (
                  <th key={column} className='text-left px-4 py-3 font-medium whitespace-nowrap'>
                    {column}
                  </th>
                ))}
                <th className='text-left px-4 py-3 font-medium'>Actions</th>
              </tr>
            </thead>
            <tbody>
              {section.rows.map((row, rowIndex) => (
                <tr key={rowIndex} className='border-t border-white/10 hover:bg-white/5 transition-colors duration-200'>
                  {renderRowValues(row).map((value, valueIndex) => (
                    <td key={valueIndex} className='px-4 py-3 whitespace-nowrap'>
                      {value}
                    </td>
                  ))}
                  <td className='px-4 py-3'>
                    <div className='flex items-center gap-2'>
                      <button className='cursor-pointer p-2 rounded-md bg-white/10 hover:bg-white/20 transition-colors duration-200'>
                        <PencilIcon className='w-4 h-4' />
                      </button>
                      <button className='cursor-pointer p-2 rounded-md bg-white/10 hover:bg-white/20 transition-colors duration-200'>
                        <Trash2Icon className='w-4 h-4' />
                      </button>
                      <button className='cursor-pointer p-2 rounded-md bg-white/10 hover:bg-white/20 transition-colors duration-200'>
                        <RotateCcwIcon className='w-4 h-4' />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className='rounded-xl border border-primary/20 bg-primary/10 p-4 flex items-start gap-3'>
        <DatabaseIcon className='w-5 h-5 mt-0.5' />
        <p className='text-sm text-gray-300'>
          Static mock data only. API integration for this page will replace local arrays with backend responses later.
        </p>
      </div>
    </div>
  )
}

export const UserManagement = () => (
  <ManagementPage title1='User' title2='Management' sectionKey='users' />
)

export const CinemaManagement = () => (
  <ManagementPage title1='Cinema' title2='Management' sectionKey='cinemas' />
)

export const ScreeningRoomManagement = () => (
  <ManagementPage title1='Screening Room' title2='Management' sectionKey='screeningRooms' />
)

export const BookingSeatManagement = () => (
  <ManagementPage title1='Booking Seat' title2='Management' sectionKey='bookingSeats' />
)

export const TicketOperations = () => (
  <ManagementPage title1='Ticket' title2='Operations' sectionKey='tickets' />
)
