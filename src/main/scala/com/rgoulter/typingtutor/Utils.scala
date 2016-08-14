package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.Token

object Utils {
  // DEBUG utility
  def dumpTokens(initToken: Token, description: String = "Tokens"){
    println(s"Dump $description:")
    var tok = initToken

    while (tok != null) {
      println(s"TOK: $tok")
      tok = tok.getNextToken()
    }
  }

}