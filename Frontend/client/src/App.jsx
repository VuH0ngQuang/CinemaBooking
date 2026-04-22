import React, { useEffect } from 'react'
import Navbar from './components/Navbar.jsx'
import {Routes, Route, useLocation} from 'react-router-dom'
import Movies from './pages/Movies.jsx'
import MovieDetails from './pages/MovieDetails.jsx'
import SeatLayout from './pages/SeatLayout.jsx'
import MyBookings from './pages/MyBookings.jsx'
import Favourite from './pages/Favourite.jsx'
import Home from './pages/Home.jsx'
import {Toaster} from 'react-hot-toast'
import Footer from './components/Footer.jsx'
import Login from './pages/Login.jsx'
import Layout from './pages/admin/Layout.jsx'
import Dashboard from './pages/admin/Dashboard.jsx'
import AddShow from './pages/admin/AddShow.jsx'
import ListShow from './pages/admin/ListShow.jsx'
import ListBooking from './pages/admin/ListBooking.jsx'
import PaymentPage from './pages/PaymentPage.jsx'
import {
  BookingSeatManagement,
  CinemaManagement,
  ScreeningRoomManagement,
  TicketOperations,
  UserManagement,
} from './pages/admin/ManagementPages.jsx'
import QrScanner from './pages/admin/QrScanner.jsx'
import AddMovie from './pages/admin/AddMovie.jsx'
import { notifyMoviesCacheUpdated } from './utils/moviesCache.js'


const App = () => {

  const isAdminRoute = useLocation().pathname.startsWith('/admin')
  const baseUrl = import.meta.env.VITE_BASE_URL

  useEffect(() => {
    if (!baseUrl) {
      console.warn('VITE_BASE_URL is not defined in .env')
      return
    }

    const moviesEndpoint = `${baseUrl.replace(/\/$/, '')}/api/movies`

    const fetchMovies = async () => {
      try {
        const response = await fetch(moviesEndpoint)
        if (!response.ok) {
          throw new Error(`Failed to fetch movies: ${response.status}`)
        }

        const movies = await response.json()
        localStorage.setItem('movies', JSON.stringify(movies))
        notifyMoviesCacheUpdated()
      } catch (error) {
        console.error('Error fetching movies:', error)
      }
    }

    fetchMovies()
  }, [baseUrl])

  return (
    <>
      <Toaster/>
      {!isAdminRoute && <Navbar/>}
      <Routes>
        <Route path='/' element={<Home/>}/>
        <Route path='/movies' element={<Movies/>}/>
        <Route path='/movies/:id' element={<MovieDetails/>}/>
        <Route path='/movies/:id/:date' element={<SeatLayout/>}/>
        <Route path='/my-bookings' element={<MyBookings/>}/>
        <Route path='/favorite' element={<Favourite/>}/>
        <Route path="/login" element={<Login />} />
        <Route path='/payment/:bookingId' element={<PaymentPage/>}/>
        <Route path='/admin/*' element={<Layout/>}>
          <Route index element={<Dashboard/>}/>
          <Route path='add-shows' element={<AddShow/>}/>
          <Route path='list-shows' element={<ListShow/>}/>
          <Route path='list-bookings' element={<ListBooking/>}/>
          <Route path='users' element={<UserManagement/>}/>
          <Route path='cinemas' element={<CinemaManagement/>}/>
          <Route path='screening-rooms' element={<ScreeningRoomManagement/>}/>
          <Route path='booking-seats' element={<BookingSeatManagement/>}/>
          <Route path='tickets' element={<TicketOperations/>}/>
          <Route path='qr-scanner' element={<QrScanner/>}/>
          <Route path='add-movie' element={<AddMovie/>}/>

        </Route>
      </Routes>
      {!isAdminRoute && <Footer/>}
    </>
  )
}

export default App