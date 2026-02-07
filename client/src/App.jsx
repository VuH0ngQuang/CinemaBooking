import React from 'react'
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


const App = () => {

  const isAdminRoute = useLocation().pathname.startsWith('/admin')

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
        <Route path='/admin/*' element={<Layout/>}>
          <Route index element={<Dashboard/>}/>
          <Route path='add-shows' element={<AddShow/>}/>
          <Route path='list-shows' element={<ListShow/>}/>
          <Route path='list-bookings' element={<ListBooking/>}/>

        </Route>
      </Routes>
      {!isAdminRoute && <Footer/>}
    </>
  )
}

export default App