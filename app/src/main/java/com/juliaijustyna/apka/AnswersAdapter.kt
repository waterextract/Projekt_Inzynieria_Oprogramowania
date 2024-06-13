package com.juliaijustyna.apka

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

data class Answer(val playerId: String, val playerName: String, val answerText: String)

class AnswersAdapter(private var answers: List<Answer>, private val playerNames: Map<String, String>) :
    RecyclerView.Adapter<AnswersAdapter.AnswerViewHolder>() {

    private val selectedAnswers = mutableMapOf<String, String>() // Mapa przechowująca wybraną odpowiedź dla każdej odpowiedzi

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_answer, parent, false)
        return AnswerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val answer = answers[position]
        holder.bind(answer, selectedAnswers[answer.playerId], playerNames)
        holder.radioGroup.setOnCheckedChangeListener(null) // Resetujemy listener, aby uniknąć przypadkowych wywołań

        // Ustawiamy wybraną odpowiedź na podstawie wcześniej zapisanych danych
        if (selectedAnswers.containsKey(answer.playerId)) {
            val selectedPlayerId = selectedAnswers[answer.playerId]
            holder.radioGroup.findViewWithTag<RadioButton>(selectedPlayerId)?.isChecked = true
        }

        // Listener do obsługi zmiany zaznaczenia w RadioGroup
        holder.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedPlayerId = group.findViewById<RadioButton>(checkedId)?.tag as? String
            if (selectedPlayerId != null) {
                // Sprawdzamy, czy już istnieje wybór dla tej odpowiedzi
                selectedAnswers[answer.playerId] = selectedPlayerId

                // Aktualizujemy wygląd RadioButtonów, aby uniknąć wielokrotnego zaznaczenia
                for (radioButtonIndex in 0 until group.childCount) {
                    val radioButton = group.getChildAt(radioButtonIndex) as? RadioButton
                    radioButton?.isEnabled = radioButton?.tag != selectedPlayerId
                }
            }
        }
    }

    override fun getItemCount() = answers.size

    fun getSelectedAnswers(): Map<String, String> {
        return selectedAnswers
    }

    class AnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radioGroup: RadioGroup = itemView.findViewById(R.id.radioGroup)
        val avatarImageView: ImageView = itemView.findViewById(R.id.avatarImageView)

        fun bind(answer: Answer, selectedPlayerId: String?, playerNames: Map<String, String>) {
            val answerTextView: TextView = itemView.findViewById(R.id.answerTextView)
            answerTextView.text = answer.answerText

            // Wypełniamy RadioGroup RadioButtonami na podstawie dostępnych graczy
            radioGroup.removeAllViews()
            playerNames.forEach { (playerId, playerName) ->
                val radioButtonView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.radio_button_with_image, radioGroup, false) as LinearLayout

                val radioButton = radioButtonView.findViewById<RadioButton>(R.id.radioButton)
                val avatarImageView = radioButtonView.findViewById<ImageView>(R.id.avatarImageView)

                radioButton.text = playerName
                radioButton.tag = playerId

                // Pobierz avatar użytkownika na podstawie playerId
                val storageRef = FirebaseStorage.getInstance().reference
                val imageRef = storageRef.child("profile_images").child("$playerId.jpg")

                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Pobierz adres URL obrazu
                    val imageUrl = downloadUri.toString()
                    // Załaduj obraz za pomocą Picasso
                    Picasso.get().load(imageUrl).into(avatarImageView)
                }.addOnFailureListener { exception ->
                    // Obsługa błędu podczas pobierania adresu URL obrazu
                }

                radioGroup.addView(radioButtonView)
            }
        }
    }
}
