import React from 'react'
import { assets } from '../assets/assets'

const Footer = () => {
  return (
    <footer className="px-6 md:px-16 lg:px-36 mt-40 w-full text-gray-300">
            <div className="flex flex-col md:flex-row justify-between w-full gap-10 border-b border-gray-500 pb-14">
                <div className="md:max-w-156">
                    <img className='w-36 h-auto' src={assets.logo}/>
                    <p className="mt-6 text-sm break-all">
                        8J+TniBBbG8sIGVtIGPDsyBwaOG6o2kgVsWpIGtow7RuZz8K8J+YqCBVaSBWxakgxqFp4oCmIGVtIMSR4burbmcgY8OzIGNo4buRaSDwn5itCvCfk4QgVGjDtG5nIHRpbiB24buBIHTDqm4g8J+nkeKAjfCfkrwsIMSR4buLYSBjaOG7iSBuaMOgIPCfj6EsIHRyxrDhu51uZyBo4buNYyDwn46TLCDhu58gxJHDonUg8J+TjSwgYuG7kSBt4bq5IHTDqm4gbMOgIGfDrCDwn5Go4oCN8J+RqeKAjfCfkafigI3wn5Gm4oCmIGFuaCBjw7MgY+G6oyDhu58gxJHDonkgcuG7k2kg8J+Xgu+4j/CflIogVsWpIGPDsyBj4bqnbiBhbmggxJHhu41jIGNobyBuZ2hlIG3hu5l0IHPhu5EgdGjDtG5nIHRpbiBraMO0bmc/4oCmIPCfkYLwn5OiCvCfpbogVsWpIMahaeKApiBlbSBjw7JuIHRy4bq7IHF1w6Eg8J+RtiwgaMahbiBjb24gYW5oIGPDsyBt4bqleSB0deG7lWkgw6Ag8J+Yogrwn6Sm4oCN4pmC77iPU2FvIFbFqSBs4bqhaSBsw6BtIHRo4bq/4oCmIPCfmJQK8J+MsSBDw7JuIGPhuqMgdMawxqFuZyBsYWkgxJHhurFuZyB0csaw4bubY+KApiDinKjwn46TCvCfj4PigI3imYLvuI/wn5OsIFbFqSB0aMOtY2ggYW5oIGNobyBuZ8aw4budaSDEkeG6v24gdOG6rW4gbmjDoCBuw7NpIGNodXnhu4duIHbhu5tpIGLhu5EgbeG6uSBlbSDEkeG6pXkgw6A/PyDwn5ik8J+PoPCfka7igI3imYLvuI8gIA== (Base64)
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
