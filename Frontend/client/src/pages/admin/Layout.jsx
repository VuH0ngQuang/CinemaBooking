import React from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import {
  Clapperboard,
  LayoutDashboard,
  List,
  ListVideo,
  MapPin,
  QrCode,
  Sofa,
  Ticket,
  Tv,
  User,
  Users,
} from 'lucide-react'

const navItems = [
  {
    label: 'Dashboard',
    path: '/admin',
    icon: LayoutDashboard,
    end: true,
  },
  {
    label: 'Add Shows',
    path: '/admin/add-shows',
    icon: Tv,
  },
  {
    label: 'List Shows',
    path: '/admin/list-shows',
    icon: ListVideo,
  },
  {
    label: 'List Bookings',
    path: '/admin/list-bookings',
    icon: List,
  },
  {
    label: 'Users',
    path: '/admin/users',
    icon: Users,
  },
  {
    label: 'Cinemas',
    path: '/admin/cinemas',
    icon: MapPin,
  },
  {
    label: 'Screening Rooms',
    path: '/admin/screening-rooms',
    icon: Sofa,
  },
  {
    label: 'Booking Seats',
    path: '/admin/booking-seats',
    icon: Ticket,
  },
  {
    label: 'Tickets',
    path: '/admin/tickets',
    icon: Ticket,
  },
  {
    label: 'QR Scanner',
    path: '/admin/qr-scanner',
    icon: QrCode,
  },
  {
    label: 'Add Movie',
    path: '/admin/add-movie',
    icon: Clapperboard,
  },
  {
    label: 'List Movies',
    path: '/admin/list-movies',
    icon: Clapperboard,
  },
]

const Layout = () => {
  return (
    <div className='min-h-screen flex bg-black text-white'>
      <aside className='w-64 border-r border-white/10 flex flex-col'>
        <div className='px-6 py-6 border-b border-white/10'>
          <Link to='/' className='text-2xl font-bold'>
            Group<span className='text-primary'>10</span>
          </Link>
        </div>

        <div className='px-6 py-7 border-b border-white/10 flex flex-col items-center'>
          <div className='w-16 h-16 rounded-full bg-primary/20 flex items-center justify-center mb-3'>
            <User className='w-8 h-8 text-white' />
          </div>
          <p className='font-medium'>Admin User</p>
        </div>

        <nav className='flex-1 px-3 py-4 space-y-2 overflow-y-auto'>
          {navItems.map((item) => {
            const Icon = item.icon

            return (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-lg px-4 py-3 transition-colors ${
                    isActive
                      ? 'bg-primary/20 text-primary border border-primary/30'
                      : 'text-gray-300 hover:bg-white/5 hover:text-white'
                  }`
                }
              >
                <Icon className='w-5 h-5' />
                <span>{item.label}</span>
              </NavLink>
            )
          })}
        </nav>
      </aside>

      <main className='flex-1 p-6 overflow-x-hidden'>
        <Outlet />
      </main>
    </div>
  )
}

export default Layout