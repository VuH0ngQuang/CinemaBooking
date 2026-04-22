import React from 'react'
import { assets } from '../assets/assets'

const Footer = () => {
  return (
    <footer className="px-6 md:px-16 lg:px-36 mt-40 w-full text-gray-300">
            <div className="flex flex-col md:flex-row justify-between w-full gap-10 border-b border-gray-500 pb-14">
                <div className="md:max-w-156">
                    <img className='w-36 h-auto' src={assets.logo}/>
                    <p className="mt-6 text-sm break-all">
                    G5sCAIzUSE1zO793qjwE/ccFeNIWwMVdZZw8LkvuoGnRGnjMweSa8DMgPRXcPO4XLtg8mGUi2kzm/1y6SECMxmzbG/2NQmDo06irADmhUzcGogoFBVHt23cfmvlbQxnuYyztGrQy2HBFj8SmDt9HEgo37tRryuwS0++BcuwqZwGoFhBtuQOELCWkyuzuzMNlTMHXwDvGnMiFDxuGcrm9Kj1l8GuW3omun7bAFEZQDo4M/KA8iWrugEQ9C2V/10BEFAiTHMpw3WBWBGRi7+hK2lqdsIMSlywGrh6xC+i8XCBiEhyLTYMCixiIQvnNdPT/GiijlVzFn7UlQznBdM9e0wV3qIVMlHsMRYDPVvSM5fufEiNleFxTzr/N+9KGC8MPxIR+HsZQ4t2AyjHDdlkqlA3MF+FzUexgkZqFBGfnldOrcsH+ZNTtRSo2dj6AGTyJ/bkYBLRXsPuECUou73N8eSrH3EEaORQQO3PHcq20OK+qVgdz24MlolwZ3o6ap7C3JJHzgqsVH63zZ3ixPRdzkMPVwVK+KPL2QL90ReO2D5ffchjV61Bmp8rBoRL3UwMA (Brotli)
                    </p>
                    <div className="flex items-center gap-2 mt-4">
                        <img src={assets.googlePlay} alt="google play" className="h-9 w-auto" />
                        <img src={assets.appStore} alt="app store" className="h-9 w-auto" />
                    </div>
                </div>
                <div className="flex-1 flex items-start md:justify-end gap-20 md:gap-40 md:pt-[60px]">
                    <div>
                        <h2 className="font-semibold mb-5">Group 10 - Cinema Booking System</h2>
                        <ul className="text-sm space-y-2">
                            <li><a href="#">Home</a></li>
                            <li><a href="#">About us</a></li>
                            <li><a href="#">Contact us</a></li>
                            <li><a href="#">Privacy policy</a></li>
                        </ul>
                    </div>
                    <div>
                        <h2 className="font-semibold mb-5">Get in touch</h2>
                        <div className="text-sm space-y-2">
                            <p>+1-234-567-890</p>
                            <p>contact@example.com</p>
                        </div>
                    </div>
                </div>
            </div>
            <p className="pt-4 text-center text-sm pb-5">
                Copyright {new Date().getFullYear()} © <a href="https://prebuiltui.com">Group 10 - Cinema Booking System</a>. All Right Reserved.
            </p>
    </footer>
  )
}

export default Footer
