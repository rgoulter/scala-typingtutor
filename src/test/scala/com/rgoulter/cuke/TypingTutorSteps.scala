package com.rgoulter.cuke

import cucumber.api.scala.{ScalaDsl, EN}
import cucumber.api.PendingException
import org.scalatest.Matchers
import com.rgoulter.typingtutor.Document
import com.rgoulter.typingtutor.TypingKeyListener


class TypingTutorSteps extends ScalaDsl with EN with Matchers {

  var doc: Document = _
  var typingKeyListener: TypingKeyListener = _



  Before() { scenario =>
    // Called before each scenario.
    // Run any cleanup here.
  }


  Given("""^a document to practice typing on$"""){ () =>
    // Arbitrary; any kind of document
    throw new PendingException()
  }


  Given("""^a lexable document to practice typing on$"""){ () =>
    // Needs to be syntax highlightable; e.g. Java.
    throw new PendingException()
  }


  Given("""^I have typed some of it$"""){ () =>
    // Arbitrary; just correctly type some of it.
    throw new PendingException()
  }


  Then("""^the document should be highlighted up to this point$"""){ () =>
    // Check tokens in doc. after offset are not Unstyled.
    throw new PendingException()
  }


  Then("""^the document should not highlighted further than this$"""){ () =>
    // Check tokens in doc. after offset are Unstyled.
    throw new PendingException()
  }


  When("""^I type in the expected characters$"""){ () =>
    // XXX Implicit constraint on input:
    //   An additional assumption here (used in SyntaxHighlighting feature)
    //   is that the Cursor passes over some comments/whitespace.

    throw new PendingException()
  }


  // XXX This is very similar to "I type in the expected characters";
  // More than code dup; inconsistent terminology is the problem.
  // What do we input? Keys or characters?
  When("""^I type in the correct keys$"""){ () =>
    throw new PendingException()
  }


  When("""^I type in several incorrect keys$"""){ () =>
    throw new PendingException()
  }


  // XXX Here we don't specify how much it advances by!
  Then("""^the cursor should advance$"""){ () =>
    throw new PendingException()
  }


  Then("""^it should skip over comments and extra whitespace$"""){ () =>
    // XXX Implicit constraint on input:
    //   i.e. that there are comments and whitespace to skip over!
    throw new PendingException()
  }


  Then("""^the user interface should reflect this$"""){ () =>
    // AFAICT, just check state of Cursor, & that RSynTA or so has this?

    // n.b. I'm not particularly sure how to check/assert that the *visual*
    // highlighting in RSyntaxTextArea is as we expect it to be.
    //
    // e.g. RSyntaxTextArea only refreshes the syntax highlighting where the
    // text changes, and our code manually forces it to refresh, somehow.
    //
    // -- My point is, how to assert here that the presented styles
    // did change?
    // It may be that checking
    //    RSyntaxDocument.iterator(): Iterator<Token>
    // is enough. (Exploratory Unit testing may show this).
    throw new PendingException()
  }


  Then("""^the cursor should indicate an error$"""){ () =>
    // AFAICT, this means
    //   Cursor.numIncorrect > 0
    throw new PendingException()
  }
}
