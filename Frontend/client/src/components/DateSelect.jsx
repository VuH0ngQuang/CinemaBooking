import React from 'react'
import BlurCircle from './BlurCircle'
import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-react'
import toast from 'react-hot-toast'
import { useNavigate } from 'react-router-dom'

const DateSelect = ({dateTime, id}) => {

  const navigate = useNavigate()  
  const [selected, setSelected] = React.useState(null);
  const availableDates = Object.keys(dateTime || {})
  const hasDates = availableDates.length > 0

  const onBookHandler = () => {
    if (!hasDates) {
        return toast('No showtime available for this movie yet')
    }
    if (!selected) {
        return toast('Please select a date')
    }
    navigate(`/movies/${id}/${selected}`)
    scrollTo(0,0)
  }
  return (
    <div id='dateSelect' className='pt-30'>
        <div className='flex flex-col md:flex-row items-center justify-between gap-10
        relative p-8 bg-primary/10 border border-primary/20 rounded-lg'>
            <BlurCircle top="-100px" left="-100px"/>
            <BlurCircle top="100px" right="0px"/>
            <div>
                <p className='text-lg font-semibold'>Choose Date</p>
                <div className='flex items-center gap-6 text-sm mt-5'>
                    <ChevronLeftIcon width={28} className="cursor-pointer"/>

                    <div className='flex gap-4 max-w-lg overflow-x-auto'>
                        {availableDates.map((date) => (
                        <button
                            onClick={() => setSelected(date)}
                            key={date}
                            className={`flex flex-col items-center justify-center
                            h-14 w-14 rounded-md transition cursor-pointer shrink-0
                            ${
                                selected === date
                                ? "bg-primary text-white font-semibold scale-105"
                                : "bg-white/10 border border-primary/70 hover:bg-primary/30"
                            }`}
                            >
                            <span className="font-semibold">
                                {new Date(date).getDate()}
                            </span>
                            <span className={`text-xs ${selected === date ? "text-white" : "text-gray-300"}`}>
                                {new Date(date).toLocaleDateString("en-US", { month: "short" })}
                            </span>
                        </button>

                        ))}
                    </div>

                    <ChevronRightIcon width={28} className="cursor-pointer"/>
                </div>
                {!hasDates && (
                    <p className='text-sm text-gray-300 mt-3'>
                        No showtimes available yet. Please add showtime data in backend/admin.
                    </p>
                )}

            </div>
            <button
              onClick={onBookHandler}
              disabled={!hasDates}
              className='bg-primary text-white px-8 py-2 mt-6 rounded hover:bg-primary/90 transition-all cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed'
            >
              Book Now
            </button>
        </div>
    </div>
  )
}

export default DateSelect
