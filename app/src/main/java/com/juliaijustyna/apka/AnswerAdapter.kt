package com.juliaijustyna.apka
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Answer(val playerId: String, val answerText: String)


class AnswersAdapter(private var answers: List<Answer>) : RecyclerView.Adapter<AnswersAdapter.AnswerViewHolder>() {

    inner class AnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //val playerIdTextView: TextView = itemView.findViewById(R.id.playerIdTextView)
        val answerTextView: TextView = itemView.findViewById(R.id.answerTextView)
    }

    fun updateAnswers(newAnswers: List<Answer>) {
        answers = newAnswers.toMutableList()
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_answer, parent, false)
        return AnswerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val currentAnswer = answers[position]
       // holder.playerIdTextView.text = currentAnswer.playerId
        holder.answerTextView.text = currentAnswer.answerText
    }

    override fun getItemCount(): Int {
        return answers.size
    }


}