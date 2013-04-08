//Author: Stephen Pryor
//March 1, 2013

package jarvis.nlp

/**
 * The TrigramModel class implements a trigram language model using
 * interpolated Kneser-Ney smoothing between trigram, bigram and unigram 
 * models. This code is based on the Perl code found in:
 *   
 * Joshua T. Goodman, "A Bit of Progress in Language Modeling", August 2001
 * 
 * @param discount the amount of discounting you want the model to perform
 * @param alpha is the value to use for laplace alpha smoothing on the unigrams    
 */
class TrigramModel(discount: Double, alpha: Double) {
  import collection.mutable.HashMap  
  
  //Trigram Denominator Counts
  private[this] val TD = HashMap[String,Double]().withDefaultValue(0.0)
  //Trigram Numerator Counts
  private[this] val TN = HashMap[String,Double]().withDefaultValue(0.0)
  //Trigram Non-zero Counts
  private[this] val TZ = HashMap[String,Double]().withDefaultValue(0.0)
  
  //Bigram Denominator Counts
  private[this] val BD = HashMap[String,Double]().withDefaultValue(0.0)
  //Bigram Numerator Counts
  private[this] val BN = HashMap[String,Double]().withDefaultValue(0.0)
  //Bigram Non-zero Counts
  private[this] val BZ = HashMap[String,Double]().withDefaultValue(0.0)
  
  //Unigram Denominator Counts
  private[this] var UD = 0.0
  //Unigram Numerator Counts
  private[this] val UN = HashMap[String,Double]().withDefaultValue(0.0)
  
  /**
   * Returns the log probability of a tokenized sentence
   * Equivalent to running the prob method 
   *
   * @param sentence is a tokenized sentence
   * @return A double of the log probability of the sentence
   */
  def apply(sentence: IndexedSeq[String]) = {
    prob(sentence)
  }
  
  /**
   * Returns the perplexity score of the model on a given dataset
   *
   * @param sentences is an indexed sequence of tokenized sentences
   * @return A double of the perplexity score of the model for the given data
   */
  def computePerplexity(sentences: IndexedSeq[IndexedSeq[String]]) = {
    var numtokens = 0
    var totalProb = 0.0
    sentences.foreach(sentence => {
      numtokens += sentence.length + 2
      totalProb += prob(sentence)
    })
    math.pow(2, -totalProb/numtokens)  
  }
  
  /**
   * Trains the model by collecting the counts from the sentences
   *
   * @param sentences is an indexed sequence of tokenized sentences
   */
  def train(sentences: IndexedSeq[IndexedSeq[String]]) {
    sentences.foreach(sentence => {
      var (w2, w1) = ("<s>", "<s>")
      sentence.map(w0 => {
        TD(w2 +" "+ w1) += 1
        val TNvalue = TN(w2 +" "+ w1 +" "+ w0)
        TN(w2 +" "+ w1 +" "+ w0) += 1
        if(TNvalue == 0.0) {
          TZ(w2 +" "+ w1) += 1
          
          BD(w1) += 1
          val BNvalue = BN(w1 +" "+ w0)
          BN(w1 +" "+ w0) += 1
          if(BNvalue == 0.0) {
            BZ(w1) += 1
            
            UD += 1
            UN(w0) += 1
          }
          w2 = w1
          w1 = w0
        }  
      })
    })
  }
  
  /**
   * Returns the log probability of a tokenized sentence
   *
   * @param sentence is a tokenized sentence
   * @return A double of the log probability of the sentence
   */
  def prob(sentence: IndexedSeq[String]) = {
    var (w2, w1) = ("<s>", "<s>")
    sentence.map(w0 => {
      var bigram = 0.0
      val unigram = (UN(w0) + alpha) / (UD + alpha*UN.size.toDouble)
      var prob = 0.0
      if(BD(w1) > 0.0) {
        if(BN(w1 + " " + w0) > 0.0) {
          bigram = (BN(w1 + " " + w0) - discount) / BD(w1)
        } else {
          bigram = 0.0
        }
        bigram = bigram + (BZ(w1)*discount / BD(w1)*unigram)
        
        var trigram = 0.0
        if(TD(w2 + " " + w1) > 0.0) {
          if(TN(w2 + " " + w1 + " " + w0) > 0.0) {
            trigram = (TN(w2 + " " + w1 + " " + w0) - discount) / TD(w2 + " " + w1)
          } else {
            trigram = 0
          }
          trigram = trigram + (TZ(w2 + " " + w1)*discount / TD(w2 + " " + w1)*bigram)
          prob = trigram
        } else {
          prob = bigram
        }
      } else {
        prob = unigram
      }
      w2 = w1
      w1 = w0
      math.log(prob)                 
    }).sum
  }  
}

//A helper object for creating TrigramModels
object TrigramModel{
  /**
   * Returns a trained TrigramModel
   *
   * @param sentences is an indexed sequence of tokenized sentences
   * @param discount the amount of discounting you want the model to perform
   * @param alpha is the value to use for laplace alpha smoothing on the unigrams   
   * @return A TrigramModel
   */
  def apply(sentences: IndexedSeq[IndexedSeq[String]], discount: Double = 0.5, alpha: Double = 0.5) = {
    val model = new TrigramModel(discount, alpha)
    model.train(sentences)
    model
  }
}
