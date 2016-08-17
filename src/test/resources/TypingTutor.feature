Feature: Typing Tutor
  A typing tutor let's me practice typing documents.

  Part of the expected behaviour here depends on the scheme
  we use for dealing with incorrect input.

  Scenario: Correctly type in shown document
    Given a document to practice typing on
    When I type in the correct keys
    Then the cursor should advance
    And the user interface should reflect this

  Scenario: Incorrectly type in shown document
    Given a document to practice typing on
    When I type in several incorrect keys
    Then the cursor should indicate an error
    And the user interface should reflect this
