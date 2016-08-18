Feature: Typing Tutor
  It's usual for programming development environments to
  have syntax highlighting.

  For a typing tutor, want to emphasise progression by
  only highlighting where I have typed up to.

  Scenario: Partially typed document
    Given a lexable document to practice typing on
    And I input the correct characters
    Then the marker should advance
    Then the document should be highlighted up to this point
    And the document should not highlighted further than this

  Scenario: Skip over insignificant parts of program
    It's not pertinent to practicising writing programs to
    have to type up the whitespace and comments.

    Given a lexable document to practice typing on
    When I input the expected characters, going passed whitespace/comments
    Then the marker should advance
    Then it should skip over comments and extra whitespace
