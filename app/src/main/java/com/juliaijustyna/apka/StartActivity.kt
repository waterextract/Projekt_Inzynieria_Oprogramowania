/*package com.juliaijustyna.apka

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class StartActivity {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val btn = view.findViewById<Button>(R.id.playBtn)
        btn.setOnClickListener{
            val intent = Intent(activity, MakeRoom::class.java)
            startActivity(intent)
        }
        return view
    }

}*/