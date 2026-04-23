import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { assets } from '../assets/assets'
import { MenuIcon, SearchIcon, XIcon } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import AuthModal from './AuthModal'

const Navbar = () => {

  const [isOpen, setIsOpen] = useState(false)

  const { user, logout, authModalOpen, openAuthModal, closeAuthModal } = useAuth()
  const navigate = useNavigate()
  const displayName = user?.full_name || user?.name || user?.email || "User"

  const handleLogout = async () => {
    try {
      await logout()
    } catch (error) {
      console.error("Logout failed:", error)
    }
    navigate("/")
  }

  return (
    <div className='fixed top-0 left-0 z-50 w-full flex items-center
    justify-between px-6 md:px-16 lg:px-36 py-5'>

      <Link to='/' className='max-md:flex-1'>
        <img src={assets.logo} alt="" className='w-36 h-auto'/>
      </Link>

      <div className={`max-md:absolute max-md:top-0 max-md:left-0 max-md:font-medium
      max-md:text-lg z-50 flex flex-col md:flex-row items-center
      max-md:justify-center gap-8 min-md:px-8 py-3 max-md:h-screen
      min-md:rounded-full backdrop-blur bg-black/70 md:bg-white/10 md:border
      border-gray-300/20 overflow-hidden transition-[width] duration-300
      ${isOpen ? 'max-md:w-full' : 'max-md:w-0'}`}>

        <XIcon
          className='md:hidden absolute top-6 right-6 w-6 h-6 cursor-pointer'
          onClick={() => setIsOpen(false)}
        />

        <Link onClick={() => { window.scrollTo(0,0); setIsOpen(false) }} to='/'>Home</Link>
        <Link onClick={() => { window.scrollTo(0,0); setIsOpen(false) }} to='/movies'>Movies</Link>
        {user && (
          <Link onClick={() => { window.scrollTo(0,0); setIsOpen(false) }} to='/my-bookings'>
            My Bookings
          </Link>
        )}
      </div>

      <div className='flex items-center gap-8'>
        <SearchIcon className='max-md:hidden w-6 h-6 cursor-pointer'/>

        {!user ? (
          <button
            onClick={() => openAuthModal()}
            className="px-4 py-1 sm:px-7 sm:py-2 bg-primary rounded-full cursor-pointer"
          >
            Login
          </button>
        ) : (
          <div className="flex items-center gap-4">
            <span className="text-white">{displayName}</span>
            <button
              onClick={handleLogout}
              className="px-4 py-1 sm:px-7 sm:py-2 bg-primary rounded-full cursor-pointer"
            >
              Logout
            </button>
          </div>
        )}
      </div>

      <MenuIcon
        className='max-md:ml-4 md:hidden w-8 h-8 cursor-pointer'
        onClick={() => setIsOpen(true)}
      />

      <AuthModal
        isOpen={authModalOpen}
        onClose={() => closeAuthModal()}
      />
    </div>
  )
}

export default Navbar
