package com.juliaijustyna.apka

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val btn = view.findViewById<Button>(R.id.playBtn)
        btn.setOnClickListener {
            val intent = Intent(activity, MakeRoom::class.java)
            startActivity(intent)
        }

        val btn2 = view.findViewById<Button>(R.id.joinBtn)
        btn2.setOnClickListener {
            val intent = Intent(activity, JoinRoom::class.java)
            startActivity(intent)
        }

        val btn3 = view.findViewById<Button>(R.id.questBtn)
        btn3.setOnClickListener {
            //Log.d("HomeFragment", "Button clicked")
            val intent = Intent(activity, AddQuestActivity::class.java)
            startActivity(intent)
        }

        val btn4 = view.findViewById<Button>(R.id.photoBtn)
        btn4.setOnClickListener {
            //Log.d("HomeFragment", "Button clicked")
            val intent = Intent(activity, AddPhotoActivity::class.java)
            startActivity(intent)
        }

        return view
    }



    companion object {

        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}