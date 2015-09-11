package fr.univlorraine.mondossierweb.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.vaadin.ui.Button;

import fr.univlorraine.mondossierweb.views.AccesBloqueView;
import fr.univlorraine.mondossierweb.views.AccesRefuseView;
import fr.univlorraine.mondossierweb.views.AdminView;
import fr.univlorraine.mondossierweb.views.AdressesView;
import fr.univlorraine.mondossierweb.views.AssistanceView;
import fr.univlorraine.mondossierweb.views.CalendrierMobileView;
import fr.univlorraine.mondossierweb.views.CalendrierView;
import fr.univlorraine.mondossierweb.views.ErreurView;
import fr.univlorraine.mondossierweb.views.EtatCivilView;
import fr.univlorraine.mondossierweb.views.FavorisMobileView;
import fr.univlorraine.mondossierweb.views.FavorisView;
import fr.univlorraine.mondossierweb.views.InformationsAnnuellesMobileView;
import fr.univlorraine.mondossierweb.views.InformationsAnnuellesView;
import fr.univlorraine.mondossierweb.views.InscriptionsView;
import fr.univlorraine.mondossierweb.views.ListeInscritsMobileView;
import fr.univlorraine.mondossierweb.views.ListeInscritsView;
import fr.univlorraine.mondossierweb.views.NotesDetailMobileView;
import fr.univlorraine.mondossierweb.views.NotesMobileView;
import fr.univlorraine.mondossierweb.views.NotesView;
import fr.univlorraine.mondossierweb.views.RechercheArborescenteView;
import fr.univlorraine.mondossierweb.views.RechercheMobileView;
import fr.univlorraine.mondossierweb.views.RechercheRapideView;

/**
 * @author Charlie Dubois
 * 
 * Méthodes Utiles
 */
public class Utils {

	static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	public static final int NB_MAX_RESULT_QUICK_SEARCH=5;
	
	public static final int NB_MAX_RESULT_SEARCH=70;

	public static final String TYPE_ELP = "Elément pédagogique";

	public static final String ELP = "ELP";
	
	public static final String TYPE_COL = "Collection de groupes";

	public static final String COL = "COL";
	
	public static final String TYPE_GRP = "Groupe";

	public static final String GRP = "GRP";

	public static final String TYPE_VET = "Etape";

	public static final String VET = "VET";

	public static final String TYPE_CMP = "Composante";

	public static final String CMP = "CMP";

	public static final String TYPE_ETU = "Etudiant";

	public static final String ETU = "ETU";

	public static final String TYPE_VDI = "Diplôme";

	public static final String VDI = "VDI";

	public static final String LIBELLE_WS_INSCRIPTION_PAYEE ="Paiement effectué";

	public static final String SEPARATEUR_CODE_GROUPE = ";";

	public static final String SHOW_MESSAGE_NOTES_PREFERENCE= "SHOW_MESSAGE_NOTES";
	
	public static final String SHOW_MESSAGE_NOTES_MOBILE_PREFERENCE= "SHOW_MESSAGE_MOBILE_NOTES";

	public static final String SHOW_MESSAGE_INTRO_PREFERENCE = "SHOW_MESSAGE_INTRO";
	
	public static final String SHOW_MESSAGE_INTRO_MOBILE_PREFERENCE = "SHOW_MESSAGE_MOBILE_INTRO";

	/** Durée en heure de la durée maxi de validité d'un swap utilisateur */
	public static final int NB_HEURE_DUREE_SWAP_USER = 1;
	
	//liste des vues desktop
	private static final String[] LISTE_VIEWS_DESKTOP = {
		AccesBloqueView.NAME,
		AccesRefuseView.NAME,
		AdminView.NAME,
		AdressesView.NAME,
		AssistanceView.NAME,
		CalendrierView.NAME,
		ErreurView.NAME,
		EtatCivilView.NAME,
		FavorisView.NAME,
		InformationsAnnuellesView.NAME, 
		InscriptionsView.NAME,
		ListeInscritsView.NAME,
		NotesView.NAME,
		RechercheArborescenteView.NAME,
		RechercheRapideView.NAME
		};
	
	//liste des vues mobiles
	private static final String[] LISTE_VIEWS_MOBILE = {
		AccesBloqueView.NAME,
		AccesRefuseView.NAME,
		CalendrierMobileView.NAME,
		ErreurView.NAME,
		FavorisMobileView.NAME,
		InformationsAnnuellesMobileView.NAME, 
		ListeInscritsMobileView.NAME,
		NotesDetailMobileView.NAME,
		NotesMobileView.NAME,
		RechercheMobileView.NAME};
	
	//liste des vues enseignants dont l'accès est à protéger
	private static final String[] LISTE_VIEWS_ENSEIGNANT = {RechercheRapideView.NAME,RechercheArborescenteView.NAME, FavorisView.NAME,ListeInscritsView.NAME, AssistanceView.NAME,
			FavorisMobileView.NAME, ListeInscritsMobileView.NAME,RechercheMobileView.NAME};

	/** formatage d'une date pour ne garder que jour, mois , annee*/
	public static String formatDateToString(Date d){
		if(d!=null){
			DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			return dateFormat.format(d);
		}else{
			return null;
		}
	}

	public static String getAnneeUniversitaireEnCours(String annee){
		int anneeEnCours = Integer.parseInt(annee);
		return anneeEnCours+"/"+(anneeEnCours+1);
	}

	public static String convertTypeToDisplay(String type) {
		if(type!=null){
			if(type.equals(ELP))
				return Utils.TYPE_ELP;
			if(type.equals(CMP))
				return Utils.TYPE_CMP;
			if(type.equals(VDI))
				return Utils.TYPE_VDI;
			if(type.equals(VET))
				return Utils.TYPE_VET;
			if(type.equals(COL))
				return Utils.TYPE_COL;
			if(type.equals(GRP))
				return Utils.TYPE_GRP;
			if(type.equals(ETU))
				return Utils.TYPE_ETU;
		}
		return type;
	}

	public static boolean isViewEnseignant(String viewName) {
		for(int i=0;i<LISTE_VIEWS_ENSEIGNANT.length;i++){
			if(LISTE_VIEWS_ENSEIGNANT[i].equals(viewName)){
				return true;
			}
		}
		return false;
	}
	

	
	public static boolean isViewDesktop(String viewName) {
		if(viewName!=null && viewName.contains("!")){
			viewName = viewName.replaceAll("!", "");
		}
		for(int i=0;i<LISTE_VIEWS_DESKTOP.length;i++){
			if(LISTE_VIEWS_DESKTOP[i].equals(viewName)){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isViewMobile(String viewName) {
		if(viewName!=null && viewName.contains("!")){
			viewName = viewName.replaceAll("!", "");
		}
		for(int i=0;i<LISTE_VIEWS_MOBILE.length;i++){
			if(LISTE_VIEWS_MOBILE[i].equals(viewName)){
				return true;
			}
		}
		return false;
	}

	public static boolean getBooleanFromString(String chaine) {
		if(StringUtils.hasText(chaine) && chaine.equals("O")){
				return true;
		}
		return false;
	}
	
	//Formate bouton/lien pour appli mobile
	public static void setButtonStyle(Button b){
		b.setStyleName("v-nav-button");
		b.addStyleName("v-button-multiline");
		b.addStyleName("link"); 
		b.addStyleName("v-link");
	}
	


}

