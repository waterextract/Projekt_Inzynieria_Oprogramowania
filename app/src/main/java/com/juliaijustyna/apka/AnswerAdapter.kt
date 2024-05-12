package com.juliaijustyna.apka
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Answer(val playerId: String, val answerText: String)
//data class PlayerID(val playerId: String)


class AnswersAdapter(private var answers: List<Answer>) : RecyclerView.Adapter<AnswersAdapter.AnswerViewHolder>() {

    inner class AnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerIdTextView: TextView = itemView.findViewById(R.id.playerIdTextView)
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
       holder.playerIdTextView.text = currentAnswer.playerId
        holder.answerTextView.text = currentAnswer.answerText
    }

    override fun getItemCount(): Int {
        return answers.size
    }


}

/*
class IDAdapter(private var ID: List<PlayerID>) : RecyclerView.Adapter<IDAdapter.IDViewHolder>() {

    inner class IDViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerIdTextView: TextView = itemView.findViewById(R.id.playerIdTextView)
       // val answerTextView: TextView = itemView.findViewById(R.id.answerTextView)
    }

    fun updateID(newID: List<PlayerID>) {
        ID = newID.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IDViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_answer, parent, false)
        return IDViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: IDViewHolder, position: Int) {
        val currentID = ID[position]
        holder.playerIdTextView.text = currentID.playerId
        //holder.answerTextView.text = currentAnswer.answerText
    }

    override fun getItemCount(): Int {
        return ID.size
    }
}
*/
