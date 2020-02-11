/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.customskill;

import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;   
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;





/*
 * This class is the actual skill.
 */
public class AlexaSkillSpeechlet implements SpeechletV2{
	
	static Logger logger = LoggerFactory.getLogger(AlexaSkillSpeechlet.class);
	
	public static String userRequest;
	
			
	private static String confirmEnglish = "Ab jetzt werde ich alle Wörter von Englisch auf Deutsch übersetzen, bis du fertig sagst.  Viel Spaß beim Lesen!";
	private static String confirmDeutsch = "Ab jetzt werde ich alle Wörter von Deutsch auf Englisch übersetzen, bis du fertig sagst.  Viel Spaß beim Lesen!";
	
	private static String confirmQuizWahl = "Willkommen beim Quiz. Willst du die englischen Wörter oder deutschen Wörter üben? ";
	
	
	private static String question;
	private static String correctAnswer;
	private static String frage;
	private static String richtigeAntwort;
	private static int count;
	
	
	
	
	private static enum RecognitionState {Words,Wort,Auswahl,QuizWahl, EnglishDeutsch, YesNoWords,YesNoWort, QuizWords, QuizWort,NeuWeiter};
	private RecognitionState recState;
	private static enum UserIntent {Lesen,Englisch, Deutsch, Quiz, Yes, No, Error, Fertig, Neu, Weiter};
	static UserIntent ourUserIntent;

	static String welcomeMsg = "Hallo, ein Neustart oder weiterlesen ?";
	static String confirmNeu = "Möchtest du  Lesen oder Quiz spielen?";
	static String confirmLesen="Auf welche Sprache möchtest du lesen? Englisch oder Deutsch?";
	static String continueQuiz = "Ich habe für dich einen Vokabeltränner vorbereitet. Möchtest du das spielen?";
	
	static String goodbyeMsg = "Danke, und bis zum nächsten Mal!";
	
	static String errorEnglishDeutschMsg = "Das habe ich nicht verstanden. Sagst du bitte Englisch oder Deutsch.";
	static String errorWahlMsg = "Das habe ich nicht verstanden. Sagst du bitte Lesen oder Quiz.";
	static String errorstartMsg = "Das habe ich nicht verstanden, Bitte Neustart oder Weiter sagen .";
	static String errorYesNoMsg = "Das habe ich nicht verstanden, Sagst du bitte Ja oder Nein.";
	static String errorNoWords= "Es gibt noch keine Wörter im Datenbank, Fang erst mal mit dem Lesen an! Auf welche Sprache möchtest du lesen? Englisch oder Deutsch?";
	static String errorAllWord= "Du hast alle Wörter geübt. Danke und bis zum nächsten Mal!";
	
	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope)
	{
		logger.info("Alexa session begins");
		
	
		recState = RecognitionState.NeuWeiter;
	
	}

	@Override
	public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope)
	{
		
		return askUserResponse(welcomeMsg);
	}
	
	//Fragen für die gefragten englischen Wörter
	private void selectQuestion() throws Exception{
		
		String fromLang = "en";
		String toLang = "de";
		
		
		Connection con = DBSqlite.createConnection();
		
		question = "Was bedeutet "+DBSqlite.selectWords(con,count)+" ?"; correctAnswer = (Translator.translate(fromLang, toLang,DBSqlite.selectWords(con,count))).toLowerCase();
		count--;
	}
	
	// Fragen für die gefragten deutschen Wörter
	private void Fragen() throws Exception{
		
		String fromLang = "de";
		String toLang = "en";
		
		Connection con = DBSqlite.createConnection();
		
		frage = "What does  "+DBSqlite.selectWort(con,count)+" mean ?"; richtigeAntwort = (Translator.translate(fromLang, toLang,DBSqlite.selectWort(con,count))).toLowerCase();
		count--;
	}
	
	


	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope)
	{
		IntentRequest request = requestEnvelope.getRequest();
		Intent intent = request.getIntent();
		
		userRequest = replaceUmlaute(intent.getSlot("anything").getValue());
		Connection con = null;
		
		
		logger.info("Received following text: [" + userRequest + "]");
		logger.info("recState is [" + recState + "]");
		SpeechletResponse resp = null;
		switch (recState) {
		
		case NeuWeiter: resp = evaluateNeuWeiter(userRequest); break;
		case Auswahl: resp = evaluateAuswahl(userRequest); break;
		case QuizWahl: resp = evaluateQuizWahl(userRequest); break;
		case EnglishDeutsch: resp = evaluateEnglishDeutsch(userRequest); break;
		case Words: try {
			
						
						resp = evaluateWords(userRequest,con);
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}; break;
		case Wort: try {
			
			
			resp = evaluateWort(userRequest,con);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}; break;
		
		case YesNoWords: resp = evaluateYesNoWords(userRequest,con); break;
		case YesNoWort: resp = evaluateYesNoWort(userRequest,con); break;
		
		case QuizWords: resp = evaluateQuizWords(userRequest); break;
		case QuizWort: resp = evaluateQuizWort(userRequest); break;
		
		
		
		default: resp = response("Erkannter Text: " + userRequest);
		}   
		return resp;
	}
	
	//Am Anfang wird Gefragt ob Weiter übersetzen oder Neustart
	private SpeechletResponse evaluateNeuWeiter(String userRequest) {
		
		Connection con = DBSqlite.createConnection();
		SpeechletResponse res = null;
		recognizeUserIntent(userRequest);
		switch (ourUserIntent) {
			case Neu: {
				res = askUserResponse(confirmNeu); 	
				recState = RecognitionState.Auswahl; break;
				
			} case Weiter: {
				
				if (DBSqlite.selectAllWords(con, DBSqlite.maxIdAllWords(con)).equals(DBSqlite.selectWords(con, DBSqlite.maxIdWords(con)))) {
					
					res = askUserResponse("Ok."); 				
					recState = RecognitionState.Words; break;
					
				}else if (DBSqlite.selectAllWords(con, DBSqlite.maxIdAllWords(con)).equals(DBSqlite.selectWort(con, DBSqlite.maxIdWort(con)))) {	
					
					res = askUserResponse("Ok."); 
					recState = RecognitionState.Wort; break;
				
					
				}else {
					res = askUserResponse(errorstartMsg); 
					recState = RecognitionState.Auswahl; break;
				}
				
			} default: {
				res = askUserResponse(errorstartMsg);
			};break;
		}
		return res;
	}
	
	/*
	 * auswählen ob Lesen oder Quiz gestartet werden soll
	 */
	private SpeechletResponse evaluateAuswahl(String userRequest) {
		SpeechletResponse res = null;
		recognizeUserIntent(userRequest);
		switch (ourUserIntent) {
			case Lesen: {

				res = askUserResponse(confirmLesen);
				recState = RecognitionState.EnglishDeutsch; break;
				
			} case Quiz: {
				res = askUserResponse(confirmQuizWahl); 				
				recState = RecognitionState.QuizWahl; break;
				
			} default: {
				res = askUserResponse(errorWahlMsg);
			};break;
		}
		return res;
	}
	
	//Quiz Evaluation für Englisch und Deutsch
	private SpeechletResponse evaluateQuizWahl(String userRequest) {
		SpeechletResponse res = null;
		recognizeUserIntent(userRequest);
		Connection con = DBSqlite.createConnection();
		switch (ourUserIntent) {
			case Englisch: {
				count = DBSqlite.maxIdWords(con);
				if (count == 0) {
					res = askUserResponse(errorNoWords);
					recState = RecognitionState.EnglishDeutsch; break;
				}else {
					try {					
						selectQuestion();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					res = askUserResponse(question);
					recState = RecognitionState.QuizWords; break;
				}
				
				
				
				
			} case Deutsch: {
				
				count = DBSqlite.maxIdWort(con);
				if (count == 0) {
					res = askUserResponse(errorNoWords);
					recState = RecognitionState.EnglishDeutsch; break;
				}else {
					try {					
						Fragen();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					res = askUserResponse(frage);
					recState = RecognitionState.QuizWort; break;
				}
				
			
			} default: {
				res = askUserResponse(errorEnglishDeutschMsg);
			};break;
		}
		return res;
	}
	
	//Buch Sprache auswählen

	private SpeechletResponse evaluateEnglishDeutsch(String userRequest) {
		SpeechletResponse res = null;
		recognizeUserIntent(userRequest);
		switch (ourUserIntent) {
			case Englisch: {

				res = askUserResponse(confirmEnglish);
				recState = RecognitionState.Words; break;
				
			} case Deutsch: {
				res = askUserResponse(confirmDeutsch); 				
				recState = RecognitionState.Wort; break;
			
			} default: {
				res = askUserResponse(errorEnglishDeutschMsg);
			};break;
		}
		return res;
	}

 /*
  * Translate words from English to german and add them to Table
  */
	private SpeechletResponse evaluateWords(String userRequest,Connection con) throws Exception {
		
		String fromLang="en";
		String toLang = "de";
		SpeechletResponse res = null;
		con = DBSqlite.createConnection();
		
		
						
		if(!userRequest.toLowerCase().equals("fertig")) {
			
			DBSqlite.createTableEnglich(con);
			DBSqlite.insertWords(con, userRequest.toLowerCase());
			DBSqlite.createTableAllWords(con);
			DBSqlite.insertAllWords(con, userRequest.toLowerCase());	
			res = askUserResponse(userRequest.toLowerCase()+" bedeutet: "+Translator.translate(fromLang, toLang, userRequest));
			
			
		}else {
			res = askUserResponse(continueQuiz); recState= RecognitionState.YesNoWords;
			count = DBSqlite.maxIdWords(con);
		}
		

		return res;
		
	}
	
	
	/*
	  * Translate words from German to English and add them to Table
	  */
	private SpeechletResponse evaluateWort(String userRequest,Connection con) throws Exception {
		
		String fromLang="de";
		String toLang = "en";
		SpeechletResponse res = null;
		con = DBSqlite.createConnection();
		
		
						
		if(!userRequest.toLowerCase().equals("fertig")) {
			
			DBSqlite.createTableDeutsch(con);
			DBSqlite.insertWort(con, userRequest.toLowerCase());
			
			DBSqlite.createTableAllWords(con);
			DBSqlite.insertAllWords(con, userRequest.toLowerCase());
			
			res = askUserResponse(userRequest.toLowerCase()+" means: "+Translator.translate(fromLang, toLang, userRequest));
			
			
		}else {
			res = askUserResponse(continueQuiz); recState= RecognitionState.YesNoWort;
			count= DBSqlite.maxIdWort(con);
		}
		

		return res;
		
	}
	
	
	/*
	 *continue Quiz or stop for English
	 */
	private SpeechletResponse evaluateYesNoWords(String userRequest,Connection con) {
		SpeechletResponse res = null;
		recognizeUserIntent(userRequest);
		switch (ourUserIntent) {
			case Yes: {
				if(count == 0) {
					res = askUserResponse(errorAllWord);
					
				}else {
					
					try {					
						selectQuestion();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					res = askUserResponse(question);
					
					recState = RecognitionState.QuizWords;
				}
				
				
			}; break;
			case No: {
				res = askUserResponse(goodbyeMsg);
				
			}; break;	
			default: {
				res = askUserResponse(errorYesNoMsg);
			}
		}
			return res;
	}
	
	
	

	/*
	 *continue Quiz or stop for German
	 */
	private SpeechletResponse evaluateYesNoWort(String userRequest,Connection con) {
		SpeechletResponse res = null;
		recognizeUserIntent(userRequest);
		switch (ourUserIntent) {
			case Yes: {
				if(count == 0) {
					res = askUserResponse(errorAllWord);
				}else {
					
					try {					
						Fragen();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					res = askUserResponse(frage);
					
					recState = RecognitionState.QuizWort;
					
				}
				
			}; break;
			case No: {
				res = askUserResponse(goodbyeMsg);				
			}; break;	
			default: {
				res = askUserResponse(errorYesNoMsg);
			}
		}
			return res;
	}
	
	
	
	private SpeechletResponse evaluateQuizWords(String userRequest) {
		SpeechletResponse res = null;
		
		if (userRequest.toLowerCase().equals(correctAnswer)) {
			
			res= askUserResponse("Richtig! Weiter?");
			
			recState = RecognitionState.YesNoWords;
			
		}else {
			
			res= askUserResponse("richtige Antwort lautet: "+correctAnswer+". Weiter?");
			recState = RecognitionState.YesNoWords;
			
		}
		return res;
	}
	
	private SpeechletResponse evaluateQuizWort(String userRequest) {
		SpeechletResponse res = null;
		
		if (userRequest.toLowerCase().equals(richtigeAntwort)) {
			
			res= askUserResponse("Richtig. Weiter?");
			
			recState = RecognitionState.YesNoWort;
			
		}else {
			
			res= askUserResponse("richtige Antwort lautet: "+richtigeAntwort+". Möchtest du weiter üben?");
			recState = RecognitionState.YesNoWort;
			
		}
		return res;
	}
	

	//TODO
	/*
	 * private void recognizeUserIntent(String userRequest) { switch
	 * (userRequest.toLowerCase()) { case "englisch": ourUserIntent =
	 * UserIntent.Englisch; break; case "deutsch": ourUserIntent =
	 * UserIntent.Deutsch; break;
	 * 
	 * case "ja": ourUserIntent = UserIntent.Yes; break; case "nein": ourUserIntent
	 * = UserIntent.No; break;
	 * 
	 * 
	 * 
	 * } logger.info("set ourUserIntent to " +ourUserIntent); }
	 */
	
	
	/*
	 * RegularExpressions recognition
	 */
	public void recognizeUserIntent(String userRequest) {
		userRequest = userRequest.toLowerCase();
		String pattern1 = "(ich möchte )?(ich will )?(ich nehme )?(auf)?deutsch( bitte)?(wörter)?(üben)?";
		String pattern2 = "(ich möchte )?(ich will )?(ich nehme )?(auf)?englisch( bitte)?(wörter)?(üben)?";
		
	
		String pattern3 = "\\bnein\\b";
		String pattern4 = "\\bja\\b";
		String pattern5 = "(ich möchte )?(ich will )?(ich nehme )?(lass uns)?quiz( bitte)?(spielen)?";
		String pattern6 = "(ich möchte )?(ich will )?(ich nehme )?(lass uns)?lesen( bitte)?";
		String pattern7 = "(ich möchte )?(ich will )?(ich nehme )?(lass uns)?neu( bitte)?(spielen)?";
		String pattern8 = "(ich möchte )?(ich will )?(ich nehme )?(lass uns)?weiter( bitte)?";

		Pattern p1 = Pattern.compile(pattern1);
		Matcher m1 = p1.matcher(userRequest);
		Pattern p2 = Pattern.compile(pattern2);
		Matcher m2 = p2.matcher(userRequest);
		Pattern p3 = Pattern.compile(pattern3);
		Matcher m3 = p3.matcher(userRequest);
		Pattern p4 = Pattern.compile(pattern4);
		Matcher m4 = p4.matcher(userRequest);
		Pattern p5 = Pattern.compile(pattern5);
		Matcher m5 = p5.matcher(userRequest);
		Pattern p6 = Pattern.compile(pattern6);
		Matcher m6 = p6.matcher(userRequest);
		Pattern p7 = Pattern.compile(pattern7);
		Matcher m7 = p7.matcher(userRequest);
		Pattern p8 = Pattern.compile(pattern8);
		Matcher m8 = p8.matcher(userRequest);
		
		if (m1.find()) {
			ourUserIntent = UserIntent.Deutsch;

		} else if (m2.find()) {
			ourUserIntent = UserIntent.Englisch;
		} else if (m3.find()) {
			ourUserIntent = UserIntent.No;
		} else if (m4.find()) {
			ourUserIntent = UserIntent.Yes;
		} else if (m5.find()) {
			ourUserIntent = UserIntent.Quiz;
		} else if (m6.find()) {
			ourUserIntent = UserIntent.Lesen;
		} else if (m7.find()) {
			ourUserIntent = UserIntent.Neu;
		} else if (m8.find()) {
			ourUserIntent = UserIntent.Weiter;
		} else {
			ourUserIntent = UserIntent.Error;
		}
		logger.info("set ourUserIntent to " +ourUserIntent);
	}

	/**
	 * formats the text in weird ways
	 * @param text
	 * @param i
	 * @return
	 */
	

	@Override
	public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope)
	{
		logger.info("Alexa session ends now");
	}



	/**
	 * Tell the user something - the Alexa session ends after a 'tell'
	 */
	private SpeechletResponse response(String text)
	{
		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(text);

		return SpeechletResponse.newTellResponse(speech);
	}

	/**
	 * A response to the original input - the session stays alive after an ask request was send.
	 *  have a look on https://developer.amazon.com/de/docs/custom-skills/speech-synthesis-markup-language-ssml-reference.html
	 * @param text
	 * @return
	 */
	private SpeechletResponse askUserResponse(String text)
	{
		
		SsmlOutputSpeech speech = new SsmlOutputSpeech();
		speech.setSsml("<speak>" + text + "</speak>");

		// reprompt after 8 seconds
		SsmlOutputSpeech repromptSpeech = new SsmlOutputSpeech();
		repromptSpeech.setSsml("<speak><emphasis level=\"strong\">Hey!</emphasis> Bist du noch da?</speak>");

		Reprompt rep = new Reprompt();
		rep.setOutputSpeech(repromptSpeech);

		return SpeechletResponse.newAskResponse(speech, rep);
	}
	//umlaut behandeln
	private static String[][] UMLAUT_REPLACEMENTS = { { "Ä", "Ae" }, { "Ü", "Ue" }, { "Ö", "Oe" }, { "ä", "ae" }, { "ü", "ue" }, { "ö", "oe" }, { "ß", "ss" } };
	public static String replaceUmlaute(String orig) {
		    String result = orig;

		    for (int i = 0; i < UMLAUT_REPLACEMENTS.length; i++) {
		        result = result.replaceAll(UMLAUT_REPLACEMENTS[i][0], UMLAUT_REPLACEMENTS[i][1]);
		    }

		    return result;
		}


}
