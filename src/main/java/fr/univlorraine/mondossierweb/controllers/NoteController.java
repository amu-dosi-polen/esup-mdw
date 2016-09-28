/**
 *
 *  ESUP-Portail MONDOSSIERWEB - Copyright (c) 2016 ESUP-Portail consortium
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package fr.univlorraine.mondossierweb.controllers;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.vaadin.server.StreamResource;

import fr.univlorraine.mondossierweb.MainUI;
import fr.univlorraine.mondossierweb.beans.ElementPedagogique;
import fr.univlorraine.mondossierweb.beans.Etape;
import fr.univlorraine.mondossierweb.beans.Etudiant;
import fr.univlorraine.mondossierweb.beans.Resultat;
import fr.univlorraine.mondossierweb.entities.apogee.Signataire;
import fr.univlorraine.mondossierweb.services.apogee.MultipleApogeeService;
import fr.univlorraine.mondossierweb.utils.PropertyUtils;

/**
 * Gestion des notes
 */
@Component
public class NoteController {

	private Logger LOG = LoggerFactory.getLogger(NoteController.class);

	/**
	 * outputstream size.
	 */
	private static final int OUTPUTSTREAM_SIZE = 1024;
	/**
	 * marge.
	 */
	private static final float MARGE_PDF = 1.0f;
	/**
	 * l'écartement du pied de page (libellé de la promo et date d'édition) des pdf.
	 */
	private static final int ECARTEMENT_PIED_PAGE_PDF = 120;

	/* Injections */
	@Resource
	private transient ApplicationContext applicationContext;
	@Resource
	private transient Environment environment;
	@Resource
	private transient EtudiantController etudiantController;
	@Resource
	private MultipleApogeeService multipleApogeeService;
	@Resource
	private transient ConfigController configController;



	/**
	 * 
	 * @return le fichier pdf du résumé des notes.
	 */
	public com.vaadin.server.Resource exportPdfResume() {



		String nomFichier = applicationContext.getMessage("pdf.notes.title", null, Locale.getDefault())+" " + MainUI.getCurrent().getEtudiant().getNom().replace('.', ' ').replace(' ', '_') + ".pdf";
		nomFichier = nomFichier.replaceAll(" ","_");

		StreamResource.StreamSource source = new StreamResource.StreamSource() {
			private static final long serialVersionUID = 1L;

			@Override
			public InputStream getStream() {
				try {
					ByteArrayOutputStream baosPDF = new ByteArrayOutputStream(OUTPUTSTREAM_SIZE);
					PdfWriter docWriter = null;
					boolean notesPDFFormatPortrait=configController.isAffichagePdfNotesFormatPortrait();
					Document document = configureDocument(MARGE_PDF,notesPDFFormatPortrait);
					docWriter = PdfWriter.getInstance(document, baosPDF);
					docWriter.setEncryption(null, null, PdfWriter.AllowPrinting, PdfWriter.ENCRYPTION_AES_128);
					docWriter.setStrictImageSequence(true);
					if(configController.isInsertionFiligranePdfNotes()){
						docWriter.setPageEvent(new Watermark(notesPDFFormatPortrait));
					}
					creerPdfResume(document,MainUI.getCurrent().getEtudiant(),notesPDFFormatPortrait);
					docWriter.close();
					baosPDF.close();
					//Creation de l'export
					byte[] bytes = baosPDF.toByteArray();
					return new ByteArrayInputStream(bytes);
				} catch (DocumentException e) {
					LOG.error("Erreur à la génération du résumé des notes : DocumentException ",e);
					return null;
				} catch (IOException e) {
					LOG.error("Erreur à la génération du résumé des notes : IOException ",e);
					return null;
				}

			}
		};

		// Création de la ressource 
		StreamResource resource = new StreamResource(source, nomFichier);
		resource.setMIMEType("application/force-download;charset=UTF-8");
		resource.setCacheTime(0);
		return resource;
	}

	/**
	 * 
	 * @return le fichier pdf du detail des notes.
	 */
	public com.vaadin.server.Resource exportPdfDetail(Etape etape) {



		String nomFichier = applicationContext.getMessage("pdf.detail.title", null, Locale.getDefault())+" " + MainUI.getCurrent().getEtudiant().getNom().replace('.', ' ').replace(' ', '_') + ".pdf";
		nomFichier = nomFichier.replaceAll(" ","_");

		StreamResource.StreamSource source = new StreamResource.StreamSource() {
			private static final long serialVersionUID = 1L;

			@Override
			public InputStream getStream() {
				try {
					ByteArrayOutputStream baosPDF = new ByteArrayOutputStream(OUTPUTSTREAM_SIZE);
					PdfWriter docWriter = null;
					boolean notesPDFFormatPortrait=configController.isAffichagePdfNotesFormatPortrait();
					Document document = configureDocument(MARGE_PDF,notesPDFFormatPortrait);
					docWriter = PdfWriter.getInstance(document, baosPDF);
					docWriter.setEncryption(null, null, PdfWriter.AllowPrinting, PdfWriter.ENCRYPTION_AES_128);
					docWriter.setStrictImageSequence(true);
					//récupération d'une eventuelle signature
					String codSign=getCodeSignataire(etape,MainUI.getCurrent().getEtudiant());
					//Si on doit mettre le filigramme et qu'on n'a pas de signature à apposer au document
					if(configController.isInsertionFiligranePdfNotes() && !StringUtils.hasText(codSign)){
						//On ajoute le filigramme
						docWriter.setPageEvent(new Watermark(notesPDFFormatPortrait));
					}
					creerPdfDetail(document,MainUI.getCurrent().getEtudiant(), etape,notesPDFFormatPortrait,codSign);
					docWriter.close();
					baosPDF.close();
					//Creation de l'export
					byte[] bytes = baosPDF.toByteArray();
					return new ByteArrayInputStream(bytes);
				} catch (DocumentException e) {
					LOG.error("Erreur à la génération du détail des notes : DocumentException ",e);
					return null;
				} catch (IOException e) {
					LOG.error("Erreur à la génération du détail des notes : IOException ",e);
					return null;
				}

			}
		};

		// Création de la ressource 
		StreamResource resource = new StreamResource(source, nomFichier);
		resource.getStream().setParameter("Content-Disposition", "attachment; filename="+nomFichier);
		//resource.setMIMEType("application/unknow");
		resource.setMIMEType("application/force-download;charset=UTF-8");
		resource.setCacheTime(0);
		return resource;
	}

	/**
	 * configure le document pdf.
	 * @param width
	 * @param height
	 * @param margin
	 * @return doc
	 */
	private Document configureDocument(final float margin, boolean notesPDFFormatPortrait) {

		Document document = new Document();

		if(notesPDFFormatPortrait){
			document.setPageSize(PageSize.A4);
		}else{
			document.setPageSize(PageSize.A4.rotate());
		}
		float marginPage = (margin / 2.54f) * 72f;
		document.setMargins(marginPage, marginPage, marginPage, marginPage);

		return document;
	}


	/**
	 * 
	 * @param document pdf
	 */
	public void creerPdfResume(final Document document, Etudiant etudiant, boolean formatPortrait) {



		//configuration des fonts
		Font normal = FontFactory.getFont(FontFactory.TIMES_ROMAN, 10, Font.NORMAL);
		Font normalbig = FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD);
		Font legerita = FontFactory.getFont(FontFactory.TIMES_ROMAN, 9, Font.ITALIC);
		Font headerbig = FontFactory.getFont(FontFactory.TIMES_ROMAN, 16, Font.BOLD);
		Font header = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD);

		if(formatPortrait){
			normal = FontFactory.getFont("Arial", 8, Font.NORMAL);
			normalbig = FontFactory.getFont("Arial", 8, Font.BOLD);
			legerita = FontFactory.getFont("Arial", 7, Font.ITALIC);
			headerbig = FontFactory.getFont("Arial", 16, Font.BOLD);
			header = FontFactory.getFont("Arial", 11, Font.BOLD);
		}

		Color headerColor = new Color(153, 153, 255);
		if(formatPortrait){
			headerColor = new Color(142, 142, 142);
		}




		//pieds de pages:
		Date d = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		String date = dateFormat.format(d);
		//alignement des libellé du pied de page:
		String partie1 = applicationContext.getMessage("pdf.notes.title", null, Locale.getDefault()); 
		String partie2 = applicationContext.getMessage("pdf.edition.date", null, Locale.getDefault())+ " : " + date;
		if (partie1.length() < ECARTEMENT_PIED_PAGE_PDF) {
			int diff = ECARTEMENT_PIED_PAGE_PDF - partie1.length();
			for (int i = 0; i < diff; i++) {
				partie1 = partie1 + " ";

			}
		} 
		if (partie2.length() < ECARTEMENT_PIED_PAGE_PDF) {
			int diff = ECARTEMENT_PIED_PAGE_PDF - partie2.length();
			for (int i = 0; i < diff; i++) {
				partie2 = " " + partie2;
			}
		}

		//creation du pied de page:
		Phrase phra = new Phrase(partie1 + " -" + applicationContext.getMessage("pdf.page", null, Locale.getDefault()), legerita);
		Phrase phra2 = new Phrase("- "+partie2, legerita);
		HeaderFooter hf = new HeaderFooter(phra, phra2);
		hf.setAlignment(HeaderFooter.ALIGN_CENTER);
		if(!formatPortrait){
			document.setFooter(hf);	 
		}



		//ouverte du document.
		document.open();
		try {
			//ajout image test
			if (configController.getLogoUniversitePdf()!= null && !configController.getLogoUniversitePdf().equals("")){
				Image image1 = Image.getInstance(configController.getLogoUniversitePdf());
				float scaleRatio = 40 / image1.getHeight();
				float newWidth=scaleRatio * image1.getWidth();
				image1.scaleAbsolute(newWidth, 40);
				if(formatPortrait){
					image1.setAbsolutePosition(560 - newWidth,760);
				}else{
					image1.setAbsolutePosition(800 - newWidth, 528);
				}
				document.add(image1);
			}

			boolean affMentionEtudiant = configController.isAffMentionEtudiant();

			//nouveau paragraphe
			Paragraph p = new Paragraph(applicationContext.getMessage("pdf.notes.title", null, Locale.getDefault()).toUpperCase(Locale.getDefault()) + "\n\n", headerbig);
			p.setIndentationLeft(5);
			if(!formatPortrait)
				p.setIndentationLeft(10);
			document.add(p);

			// Phrase pour le header
			Phrase phraheader = new Phrase("",normal);	

			if(formatPortrait) {
				// PFE : Ajout Université
				Paragraph p000 = new Paragraph(multipleApogeeService.getLibEtablissementDef()+"\n", normalbig);
				p000.setIndentationLeft(5);
				if(!formatPortrait)
					p000.setIndentationLeft(10);
				document.add(p000);
				phraheader.add(p000);
			}

			if (etudiant.getNom() != null) {
				Paragraph p0 = new Paragraph(etudiant.getNom(), normal);
				p0.setIndentationLeft(5);
				if(!formatPortrait)
					p0.setIndentationLeft(10);
				document.add(p0);
			}
			if (etudiant.getCod_etu() != null) {
				Paragraph p01 = new Paragraph(applicationContext.getMessage("pdf.folder", null, Locale.getDefault()) + " : " + etudiant.getCod_etu(), normal);
				p01.setIndentationLeft(5);
				if(!formatPortrait)
					p01.setIndentationLeft(10);
				document.add(p01);
			}
			if (!formatPortrait) {
				if (etudiant.getCod_nne() != null) {
					Paragraph p02 = new Paragraph(applicationContext.getMessage("pdf.nne", null, Locale.getDefault()) + " : " + etudiant.getCod_nne(), normal);
					p02.setIndentationLeft(5);
					if(!formatPortrait)
						p02.setIndentationLeft(10);
					document.add(p02);
				}
				if (etudiant.getEmail() != null) {
					Paragraph p03 = new Paragraph(applicationContext.getMessage("pdf.mail", null, Locale.getDefault()) +" : " + etudiant.getEmail(), normal);
					p03.setIndentationLeft(5);
					if(!formatPortrait)
						p03.setIndentationLeft(10);
					document.add(p03);
				}

				Paragraph p03 = new Paragraph(applicationContext.getMessage("pdf.edition.date", null, Locale.getDefault()) + " : " + date, normal);
				p03.setIndentationLeft(5);
				if(!formatPortrait)
					p03.setIndentationLeft(10);
				document.add(p03);
				document.add(new Paragraph("\n"));
			}

			if (formatPortrait) {
				HeaderFooter headerdi = new HeaderFooter(phraheader,false);
				headerdi.setAlignment(HeaderFooter.ALIGN_LEFT);
				document.setHeader(headerdi);
				document.add(new Paragraph("\n",normal));
			}

			//Partie DIPLOMES
			PdfPTable table = new PdfPTable(1);
			table.setWidthPercentage(98);
			PdfPCell cell = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.diplomes", null, Locale.getDefault()).toUpperCase(Locale.getDefault())+ " ", header));
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setBackgroundColor(headerColor);


			table.addCell(cell);

			PdfPTable table2;


			if(!etudiant.isAfficherRang()){
				table2= new PdfPTable(4);
			}else{
				table2 = new PdfPTable(5);
			}

			table2.setWidthPercentage(98);

			int tailleColonneLib = 110;
			if(affMentionEtudiant)
				tailleColonneLib = 90;

			if(!etudiant.isAfficherRang()){
				int [] tabWidth = {26,35,tailleColonneLib,70};
				table2.setWidths(tabWidth);
			}else{
				int [] tabWidth = {26,35,tailleColonneLib - 5,70,15};
				table2.setWidths(tabWidth);
			}



			Paragraph p1 = new Paragraph(applicationContext.getMessage("pdf.year", null, Locale.getDefault()),normalbig);
			Paragraph p2 = new Paragraph(applicationContext.getMessage("pdf.code.vers", null, Locale.getDefault()),normalbig);
			Paragraph p3 = new Paragraph(applicationContext.getMessage("pdf.diplome", null, Locale.getDefault()),normalbig);


			PdfPCell ct1 = new PdfPCell(p1);
			PdfPCell ct2 = new PdfPCell(p2);
			PdfPCell ct3 = new PdfPCell(p3);

			ct1.setBorder(Rectangle.BOTTOM); ct1.setBorderColorBottom(Color.black);
			ct2.setBorder(Rectangle.BOTTOM); ct2.setBorderColorBottom(Color.black);
			ct3.setBorder(Rectangle.BOTTOM); ct3.setBorderColorBottom(Color.black);



			table2.addCell(ct1);
			table2.addCell(ct2);
			table2.addCell(ct3);

			PdfPTable table21;
			if(!affMentionEtudiant){
				table21 = new PdfPTable(3);
				int [] tabWidth21 = {25,20,25};
				table21.setWidths(tabWidth21);
			}else{
				table21 = new PdfPTable(4);
				int [] tabWidth21 = {25,20,25,20};
				table21.setWidths(tabWidth21);
			}

			PdfPCell ct4 = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.session", null, Locale.getDefault()), normalbig));
			PdfPCell ct5 = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.note", null, Locale.getDefault()), normalbig));
			PdfPCell ct6 = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.resultat", null, Locale.getDefault()), normalbig));
			PdfPCell ctmention = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.mention", null, Locale.getDefault()), normalbig));



			ct4.setBorder(Rectangle.BOTTOM); ct4.setBorderColorBottom(Color.black);
			ct5.setBorder(Rectangle.BOTTOM); ct5.setBorderColorBottom(Color.black);
			ct6.setBorder(Rectangle.BOTTOM); ct6.setBorderColorBottom(Color.black);
			ctmention.setBorder(Rectangle.BOTTOM); ctmention.setBorderColorBottom(Color.black);



			table21.addCell(ct4);
			table21.addCell(ct5);
			table21.addCell(ct6);
			if(affMentionEtudiant){
				table21.addCell(ctmention);
			}

			PdfPCell ct7 = new PdfPCell(table21);
			ct7.setBorder(Rectangle.BOTTOM); 
			table2.addCell(ct7);

			PdfPCell ctrang  = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.rank", null, Locale.getDefault()),normalbig));
			ctrang.setBorder(Rectangle.BOTTOM); ctrang.setBorderColorBottom(Color.black);

			if(etudiant.isAfficherRang()){
				table2.addCell(ctrang);
			}


			for (int i = 0; i < etudiant.getDiplomes().size(); i++) {
				Paragraph pa = new Paragraph(etudiant.getDiplomes().get(i).getAnnee(), normal);
				PdfPCell celltext = new PdfPCell(pa);
				celltext.setBorder(Rectangle.NO_BORDER);

				Paragraph pa2 = new Paragraph(etudiant.getDiplomes().get(i).getCod_dip()+ "/" + etudiant.getDiplomes().get(i).getCod_vrs_vdi(), normal);
				PdfPCell celltext2 = new PdfPCell(pa2);
				celltext2.setBorder(Rectangle.NO_BORDER);

				Paragraph pa3 = new Paragraph(etudiant.getDiplomes().get(i).getLib_web_vdi(), normal);
				PdfPCell celltext3 = new PdfPCell(pa3);
				celltext3.setBorder(Rectangle.NO_BORDER);

				Paragraph parang = new Paragraph(etudiant.getDiplomes().get(i).getRang(), normal);
				PdfPCell cellrang = new PdfPCell(parang);
				cellrang.setBorder(Rectangle.NO_BORDER);

				PdfPCell cellvide = new PdfPCell();
				cellvide.setBorder(Rectangle.NO_BORDER);

				table2.addCell(celltext);
				table2.addCell(celltext2);
				table2.addCell(celltext3);

				PdfPTable table3;
				if(!affMentionEtudiant){
					table3 = new PdfPTable(3);
					int [] tabWidth2 = {25,20,25};
					table3.setWidths(tabWidth2);
				}else{
					table3 = new PdfPTable(4);
					int [] tabWidth2 = {25,20,25,8};
					table3.setWidths(tabWidth2);
				}

				int j = 0;
				List<Resultat> lres = etudiant.getDiplomes().get(i).getResultats();
				while (j < lres.size()) {

					Paragraph pa5 = new Paragraph(lres.get(j).getSession(), normal);
					PdfPCell celltext5 = new PdfPCell(pa5);
					celltext5.setBorder(Rectangle.NO_BORDER);
					table3.addCell(celltext5);

					if (lres.get(j).getNote() != null) {
						Paragraph pa6 = new Paragraph(lres.get(j).getNote().toString(), normal);
						PdfPCell celltext6 = new PdfPCell(pa6);
						celltext6.setBorder(Rectangle.NO_BORDER);
						table3.addCell(celltext6);
					} else {
						Paragraph pa6 = new Paragraph("", normal);
						PdfPCell celltext6 = new PdfPCell(pa6);
						celltext6.setBorder(Rectangle.NO_BORDER);
						table3.addCell(celltext6);
					}

					Paragraph pa7 = new Paragraph(lres.get(j).getAdmission(), normal);
					PdfPCell celltext7 = new PdfPCell(pa7);
					celltext7.setBorder(Rectangle.NO_BORDER);
					table3.addCell(celltext7);

					if(affMentionEtudiant){
						Paragraph pa8 = new Paragraph(lres.get(j).getCodMention(), normal);
						PdfPCell celltext8 = new PdfPCell(pa8);
						celltext8.setBorder(Rectangle.NO_BORDER);
						table3.addCell(celltext8);
					}


					j++;
				}

				PdfPCell celltext4 = new PdfPCell(table3);
				celltext4.setBorder(Rectangle.NO_BORDER);
				table2.addCell(celltext4);

				//if(config.isAffRangEtudiant()){
				if(etudiant.getDiplomes().get(i).isAfficherRang()){
					table2.addCell(cellrang);
				}else{
					//On insere une cellule vide si on affiche pas ce rang, alors que la colonne rang fait partie de la table
					if(etudiant.isAfficherRang()){
						table2.addCell(cellvide);
					}
				}


			}



			document.add(table);
			document.add(table2);
			document.add(new Paragraph("\n"));



			//Partie ETAPES
			PdfPTable tabletape = new PdfPTable(1);
			tabletape.setWidthPercentage(98);
			PdfPCell celletape = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.etapes", null, Locale.getDefault()).toUpperCase(Locale.getDefault()), header));
			celletape.setBorder(Rectangle.NO_BORDER);
			celletape.setBackgroundColor(headerColor);

			tabletape.addCell(celletape);

			PdfPTable tabletape2;


			if(!etudiant.isAfficherRang()){
				tabletape2= new PdfPTable(4);
				tabletape2.setWidthPercentage(98);
				int [] tabWidthetape = {26,35,tailleColonneLib,70};
				tabletape2.setWidths(tabWidthetape);
			}else{
				tabletape2= new PdfPTable(5);
				tabletape2.setWidthPercentage(98);
				int [] tabWidthetape = {26,35,tailleColonneLib - 5 ,70,15};
				tabletape2.setWidths(tabWidthetape);
			}


			PdfPCell ct3etape = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.etape", null, Locale.getDefault()),normalbig));
			ct3etape.setBorder(Rectangle.BOTTOM); ct3etape.setBorderColorBottom(Color.black);

			tabletape2.addCell(ct1);
			tabletape2.addCell(ct2);
			tabletape2.addCell(ct3etape);

			tabletape2.addCell(ct7);

			if(etudiant.isAfficherRang()){
				tabletape2.addCell(ctrang);
			}

			for (int i = 0; i < etudiant.getEtapes().size(); i++) {
				Paragraph pa = new Paragraph(etudiant.getEtapes().get(i).getAnnee(), normal);
				PdfPCell celltext = new PdfPCell(pa);
				celltext.setBorder(Rectangle.NO_BORDER);
				tabletape2.addCell(celltext);

				Paragraph pa2 = new Paragraph(etudiant.getEtapes().get(i).getCode()+ "/" + etudiant.getEtapes().get(i).getVersion(), normal);
				PdfPCell celltext2 = new PdfPCell(pa2);
				celltext2.setBorder(Rectangle.NO_BORDER);
				tabletape2.addCell(celltext2);

				Paragraph pa3 = new Paragraph(etudiant.getEtapes().get(i).getLibelle(), normal);
				PdfPCell celltext3 = new PdfPCell(pa3);
				celltext3.setBorder(Rectangle.NO_BORDER);
				tabletape2.addCell(celltext3);

				Paragraph parEtapeRang = new Paragraph(etudiant.getEtapes().get(i).getRang(), normal);
				PdfPCell cellEtapeRang = new PdfPCell(parEtapeRang);
				cellEtapeRang.setBorder(Rectangle.NO_BORDER);

				PdfPCell cellvide = new PdfPCell();
				cellvide.setBorder(Rectangle.NO_BORDER);

				PdfPTable table3; 

				if(!affMentionEtudiant){
					table3= new PdfPTable(3);
					int [] tabWidth2 = {25,20,25};
					table3.setWidths(tabWidth2);
				}else{
					table3= new PdfPTable(4);
					int [] tabWidth2 = {25,20,25,8};
					table3.setWidths(tabWidth2);
				}

				int j = 0;
				List<Resultat> lres = etudiant.getEtapes().get(i).getResultats();
				while (j < lres.size()) {

					Paragraph pa5 = new Paragraph(lres.get(j).getSession(), normal);
					PdfPCell celltext5 = new PdfPCell(pa5);
					celltext5.setBorder(Rectangle.NO_BORDER);
					table3.addCell(celltext5);

					if (lres.get(j).getNote() != null) {
						Paragraph pa6 = new Paragraph(lres.get(j).getNote().toString(), normal);
						PdfPCell celltext6 = new PdfPCell(pa6);
						celltext6.setBorder(Rectangle.NO_BORDER);
						table3.addCell(celltext6);
					} else {
						Paragraph pa6 = new Paragraph("", normal);
						PdfPCell celltext6 = new PdfPCell(pa6);
						celltext6.setBorder(Rectangle.NO_BORDER);
						table3.addCell(celltext6);
					}

					Paragraph pa7 = new Paragraph(lres.get(j).getAdmission(), normal);
					PdfPCell celltext7 = new PdfPCell(pa7);
					celltext7.setBorder(Rectangle.NO_BORDER);
					table3.addCell(celltext7);

					if(affMentionEtudiant){
						Paragraph pa8 = new Paragraph(lres.get(j).getCodMention(), normal);
						PdfPCell celltext8 = new PdfPCell(pa8);
						celltext8.setBorder(Rectangle.NO_BORDER);
						table3.addCell(celltext8);
					}

					j++;
				}
				PdfPCell celltext4 = new PdfPCell(table3);
				celltext4.setBorder(Rectangle.NO_BORDER);
				tabletape2.addCell(celltext4);

				//if(config.isAffRangEtudiant()){
				if(etudiant.getEtapes().get(i).isAfficherRang()){
					tabletape2.addCell(cellEtapeRang);
				}else{
					if(etudiant.isAfficherRang()){
						tabletape2.addCell(cellvide);
					}
				}

			}


			document.add(tabletape);
			document.add(tabletape2);
			document.add(new Paragraph("\n"));


			//Partie Informations
			if (etudiant.isSignificationResultatsUtilisee()) {
				PdfPTable tablequestions = new PdfPTable(1);
				tablequestions.setWidthPercentage(98);
				PdfPCell cellquestions = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.questions", null, Locale.getDefault())+ " ", header));
				cellquestions.setBorder(Rectangle.NO_BORDER);
				cellquestions.setBackgroundColor(headerColor);

				tablequestions.addCell(cellquestions);


				String grilleSignficationResultats = "";
				Set<String> ss = etudiant.getSignificationResultats().keySet();
				for(String k : ss){
					if(k != null && !k.equals("") && !k.equals(" ")){
						grilleSignficationResultats = grilleSignficationResultats + k+" : "+ etudiant.getSignificationResultats().get(k);
						grilleSignficationResultats = grilleSignficationResultats + "   ";
					}
				}

				PdfPTable tablequestions2 = new PdfPTable(1);
				tablequestions2.setWidthPercentage(98);
				PdfPCell cellquestions2 = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.code.resultat.signification", null, Locale.getDefault()) + " : \n" + grilleSignficationResultats, normal));
				cellquestions2.setBorder(Rectangle.NO_BORDER);
				tablequestions2.addCell(cellquestions2);

				document.add(tablequestions);
				document.add(tablequestions2);
			}

		} catch (BadElementException e) {
			LOG.error("Erreur à la génération du résumé des notes : BadElementException ",e);
		} catch (MalformedURLException e) {
			LOG.error("Erreur à la génération du résumé des notes : MalformedURLException ",e);
		} catch (IOException e) {
			LOG.error("Erreur à la génération du résumé des notes : IOException ",e);
		} catch (DocumentException e) {
			LOG.error("Erreur à la génération du résumé des notes : DocumentException ",e);
		}
		// step 6: fermeture du document.
		document.close();




	}



	/**
	 * 
	 * @param document pdf
	 */
	public void creerPdfDetail(final Document document, Etudiant etudiant, Etape etape, boolean formatPortrait,String codSign) {



		//configuration des fonts
		Font normal = FontFactory.getFont(FontFactory.TIMES_ROMAN, 10, Font.NORMAL);
		Font normalbig = FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD);
		Font legerita = FontFactory.getFont(FontFactory.TIMES_ROMAN, 9, Font.ITALIC);
		Font headerbig = FontFactory.getFont(FontFactory.TIMES_ROMAN, 16, Font.BOLD);
		Font header = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.BOLD);
		if (formatPortrait) {
			normal = FontFactory.getFont("Arial", 8, Font.NORMAL);
			normalbig = FontFactory.getFont("Arial", 8, Font.BOLD);
			legerita = FontFactory.getFont("Arial", 7, Font.ITALIC);
			headerbig = FontFactory.getFont("Arial", 16, Font.BOLD);
			header = FontFactory.getFont("Arial", 11, Font.BOLD);
		}

		Color headerColor = new Color(153, 153, 255);
		if(formatPortrait){
			headerColor = new Color(142, 142, 142);
		}

		//pieds de pages:
		Date d = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		String date = dateFormat.format(d);

		//alignement des libellés du pied de page:
		String partie1 = applicationContext.getMessage("pdf.notes.detail", null, Locale.getDefault()); 
		String partie2 = applicationContext.getMessage("pdf.edition.date", null, Locale.getDefault())+ " : " + date;
		if (partie1.length() < ECARTEMENT_PIED_PAGE_PDF) {
			int diff = ECARTEMENT_PIED_PAGE_PDF - partie1.length();
			for (int i = 0; i < diff; i++) {
				partie1 = partie1 + " ";

			}
		} 
		if (partie2.length() < ECARTEMENT_PIED_PAGE_PDF) {
			int diff = ECARTEMENT_PIED_PAGE_PDF - partie2.length();
			for (int i = 0; i < diff; i++) {
				partie2 = " " + partie2;
			}
		}

		//Si on doit apposer une signature
		if (configController.isNotesPDFsignature()) {

			try {
				if (StringUtils.hasText(codSign)) {
					Signataire signataire = multipleApogeeService.getSignataire(codSign,PropertyUtils.getClefApogeeDecryptBlob());
					if (signataire.getImg_sig_std() != null){
						float[] widthsSignataire = {2f, 1.3f};
						PdfPTable tableSignataire = new PdfPTable(widthsSignataire);

						tableSignataire.setWidthPercentage(100f);
						tableSignataire.addCell(makeCellSignataire("", normal));
						tableSignataire.addCell(makeCellSignataire(applicationContext.getMessage("pdf.notes.fait1", null, Locale.getDefault())+" "+configController.getNotesPDFLieuEdition()+applicationContext.getMessage("pdf.notes.fait2", null, Locale.getDefault())+" " + date , normal));
						tableSignataire.addCell(makeCellSignataire("", normal));

						tableSignataire.addCell(makeCellSignataire(signataire.getNom_sig(), normal));
						tableSignataire.addCell(makeCellSignataire("", normal));


						Paragraph para2 = new Paragraph();
						para2.add(new Phrase(applicationContext.getMessage("pdf.notes.fait1", null, Locale.getDefault())+" "+configController.getNotesPDFLieuEdition()+applicationContext.getMessage("pdf.notes.fait2", null, Locale.getDefault())+ " " + date + ", "+ signataire.getQua_sig() + " " + signataire.getNom_sig(),normal));

						try {
							Image imageSignature = Image.getInstance(signataire.getImg_sig_std());
							imageSignature.scaleAbsolute(78,46);
							PdfPCell cellSignature = new PdfPCell();
							cellSignature.setBorder(0);
							cellSignature.setImage(imageSignature);
							cellSignature.setFixedHeight(72f/(float)300 * imageSignature.getHeight());
							cellSignature.setHorizontalAlignment(Element.ALIGN_CENTER);
							tableSignataire.addCell(cellSignature);

							Chunk ck = new Chunk (imageSignature, 0, -10, true);
							para2.add(ck);

						}
						catch (IOException e){
						}

						HeaderFooter footer = new HeaderFooter(para2,false);
						footer.setAlignment(HeaderFooter.ALIGN_LEFT);
						document.setFooter(footer);
					}
				}
				else {
					Paragraph para2 = new Paragraph();
					para2.add(new Phrase(applicationContext.getMessage("pdf.notes.fait1", null, Locale.getDefault())+" "+configController.getNotesPDFLieuEdition()+applicationContext.getMessage("pdf.notes.fait2", null, Locale.getDefault())+" "+ date, normal));
					para2.add(new Phrase("\n"+applicationContext.getMessage("pdf.notes.info.original", null, Locale.getDefault()),normal));

					HeaderFooter footer = new HeaderFooter(para2,false);
					footer.setAlignment(HeaderFooter.ALIGN_LEFT);
					document.setFooter(footer);

				}
			} catch (Exception e) {
				LOG.error("Erreur lors de l'ajout de la signature sur le relevé de note ",e);
			}


		}else{
			//creation du pied de page:
			Phrase phra = new Phrase(partie1 + " -" + applicationContext.getMessage("pdf.page", null, Locale.getDefault()), legerita);
			Phrase phra2 = new Phrase("- "+partie2, legerita);
			HeaderFooter hf = new HeaderFooter(phra, phra2);
			hf.setAlignment(HeaderFooter.ALIGN_CENTER);
			document.setFooter(hf);	 
			document.setFooter(hf);
		}

		//ouverte du document.
		document.open();
		try {
			//ajout image test
			if (configController.getLogoUniversitePdf() != null && !configController.getLogoUniversitePdf().equals("")){
				Image image1 = Image.getInstance(configController.getLogoUniversitePdf());
				float scaleRatio = 40 / image1.getHeight();
				float newWidth=scaleRatio * image1.getWidth();
				image1.scaleAbsolute(newWidth, 40);
				if(formatPortrait){
					image1.setAbsolutePosition(560 - newWidth,760);
				}else{
					image1.setAbsolutePosition(800 - newWidth, 528);
				}
				document.add(image1);
			}



			//nouveau paragraphe
			Paragraph p = new Paragraph(applicationContext.getMessage("pdf.notes.title", null, Locale.getDefault()).toUpperCase(Locale.getDefault()) + "\n\n", headerbig);
			p.setIndentationLeft(5);
			if(!formatPortrait)
				p.setIndentationLeft(10);
			document.add(p);

			// Phrase pour le header
			Phrase phraheader = new Phrase("",normal);

			if (formatPortrait) {	
				// PFE : Ajout Université
				Paragraph p000 = new Paragraph(multipleApogeeService.getLibEtablissementDef()+"\n", normalbig);
				p000.setIndentationLeft(5);
				if(!formatPortrait)
					p000.setIndentationLeft(10);
				document.add(p000);
				phraheader.add(p000);
			}

			if (etudiant.getNom() != null) {
				Paragraph p0 = new Paragraph(etudiant.getNom(), normal);
				p0.setIndentationLeft(5);
				if(!formatPortrait)
					p0.setIndentationLeft(10);
				document.add(p0);
			}
			if (etudiant.getCod_etu() != null) {
				Paragraph p01 = new Paragraph(applicationContext.getMessage("pdf.folder", null, Locale.getDefault())+ " : " + etudiant.getCod_etu(), normal);
				p01.setIndentationLeft(5);
				if(!formatPortrait)
					p01.setIndentationLeft(10);
				document.add(p01);
			}

			if (!formatPortrait){
				if (etudiant.getCod_nne() != null) {
					Paragraph p02 = new Paragraph(applicationContext.getMessage("pdf.nne", null, Locale.getDefault())+ " : " + etudiant.getCod_nne(), normal);
					p02.setIndentationLeft(5);
					if(!formatPortrait)
						p02.setIndentationLeft(10);
					document.add(p02);
				}
				if (etudiant.getEmail() != null) {
					Paragraph p03 = new Paragraph(applicationContext.getMessage("pdf.mail", null, Locale.getDefault()) + " : " + etudiant.getEmail(), normal);
					p03.setIndentationLeft(5);
					if(!formatPortrait)
						p03.setIndentationLeft(10);
					document.add(p03);
				}

				Paragraph p03 = new Paragraph(applicationContext.getMessage("pdf.edition.date", null, Locale.getDefault()) + " : " + date, normal);
				p03.setIndentationLeft(5);
				if(!formatPortrait)
					p03.setIndentationLeft(10);
				document.add(p03);
				document.add(new Paragraph("\n"));
			}

			if (formatPortrait) {
				// on teste s'il y a bien des elps presents
				String annee = "";
				if (etudiant.getElementsPedagogiques().size()>0){
					annee = etudiant.getElementsPedagogiques().get(0).getAnnee().replaceAll("FICM", "");
				}
				annee = annee.replaceAll("epreuve", "");
				Paragraph pYearAmu = new Paragraph(applicationContext.getMessage("pdf.year", null, Locale.getDefault()) + " : " + annee + "                                                                                                                                                                                                     page 1", normal);
				pYearAmu.setIndentationLeft(5);
				document.add(pYearAmu);
				Phrase pAnnee = new Phrase(applicationContext.getMessage("pdf.year", null, Locale.getDefault()) + " : " + annee + "                                                                                                                                                                                                     page ", normal);
				Phrase pAfter = new Phrase(" ", normal);
				phraheader.add(pAnnee);
				HeaderFooter headerp = new HeaderFooter(phraheader,pAfter);
				headerp.setAlignment(HeaderFooter.ALIGN_LEFT);
				document.setHeader(headerp);

				document.add(new Paragraph("\n",normal));
			}


			//Partie des notes
			PdfPTable table = new PdfPTable(1);
			table.setWidthPercentage(98);
			PdfPCell cell = new PdfPCell(new Paragraph(etape.getLibelle()+" - "+applicationContext.getMessage("pdf.annee.universitaire", null, Locale.getDefault()) + " : " + etape.getAnnee(), header));
			if(formatPortrait){
				cell = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.elements.epreuves", null, Locale.getDefault()).toUpperCase(Locale.getDefault()), header));
			}
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setBackgroundColor(headerColor);

			table.addCell(cell);

			PdfPTable table2; 


			boolean afficherRangElpEpr = etudiantController.isAfficherRangElpEpr();
			boolean affRangEtudiant = configController.isAffRangEtudiant();
			boolean affECTSEtudiant =configController.isAffECTSEtudiant();



			if((!affRangEtudiant && !afficherRangElpEpr)&& !affECTSEtudiant){
				//NI isAffRangEtudiant  NI isAffECTSEtudiant
				table2= new PdfPTable(6);
				table2.setWidthPercentage(98);
				if(formatPortrait){
					int [] tabWidthPortrait = {33,90,22,15,30,22};
					table2.setWidths(tabWidthPortrait);
				}else{
					int [] tabWidth = {35,110,25,25,25,25};
					table2.setWidths(tabWidth);
				}
			}else{
				if(((affRangEtudiant || afficherRangElpEpr) && !affECTSEtudiant) ||
						((!affRangEtudiant&& !afficherRangElpEpr) && affECTSEtudiant)){
					//isAffRangEtudiant  OU isAffECTSEtudiant
					table2= new PdfPTable(7);
					table2.setWidthPercentage(98);
					if(formatPortrait){
						int [] tabWidthPortrait = {33,90,22,15,30,22,30};
						table2.setWidths(tabWidthPortrait);
					}else{
						int [] tabWidth = {33,110,22,22,22,22,15};
						table2.setWidths(tabWidth);
					}
				}else{
					//isAffRangEtudiant  ET isAffECTSEtudiant
					table2= new PdfPTable(8);
					table2.setWidthPercentage(98);

					if(formatPortrait){
						int [] tabWidthPortrait = {33,90,22,15,30,22,30,22};
						table2.setWidths(tabWidthPortrait);
					}else{
						int [] tabWidth = {33,110,22,22,22,22,15,15};
						table2.setWidths(tabWidth);
					}
				}
			}


			//Paragraph p1 = new Paragraph(applicationContext.getMessage("pdf.year", null, Locale.getDefault()),normalbig);
			Paragraph p2 = new Paragraph(applicationContext.getMessage("pdf.code", null, Locale.getDefault()),normalbig);
			Paragraph p3 = new Paragraph(applicationContext.getMessage("pdf.label", null, Locale.getDefault()),normalbig);
			Paragraph parRang = new Paragraph(applicationContext.getMessage("pdf.rank", null, Locale.getDefault()),normalbig);
			Paragraph parEcts = new Paragraph(applicationContext.getMessage("pdf.ects", null, Locale.getDefault()),normalbig);

			PdfPCell ct4 = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.session", null, Locale.getDefault()) + " 1", normalbig));
			PdfPCell ct5 = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.resultat", null, Locale.getDefault()), normalbig));
			PdfPCell ct6 = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.session", null, Locale.getDefault()) + " 2", normalbig));
			PdfPCell ct7 = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.resultat", null, Locale.getDefault()), normalbig));

			//PdfPCell ct1 = new PdfPCell(p1);
			PdfPCell ct2 = new PdfPCell(p2);
			PdfPCell ct3 = new PdfPCell(p3);
			PdfPCell cellRang = new PdfPCell(parRang);
			PdfPCell cellEcts = new PdfPCell(parEcts);

			//ct1.setBorder(Rectangle.BOTTOM); ct1.setBorderColorBottom(Color.black);
			ct2.setBorder(Rectangle.BOTTOM); ct2.setBorderColorBottom(Color.black);
			ct3.setBorder(Rectangle.BOTTOM); ct3.setBorderColorBottom(Color.black);
			ct4.setBorder(Rectangle.BOTTOM); ct4.setBorderColorBottom(Color.black);
			ct5.setBorder(Rectangle.BOTTOM); ct5.setBorderColorBottom(Color.black);
			ct6.setBorder(Rectangle.BOTTOM); ct6.setBorderColorBottom(Color.black);
			ct7.setBorder(Rectangle.BOTTOM); ct7.setBorderColorBottom(Color.black);
			cellRang.setBorder(Rectangle.BOTTOM); cellRang.setBorderColorBottom(Color.black);
			cellEcts.setBorder(Rectangle.BOTTOM); cellEcts.setBorderColorBottom(Color.black);


			if (formatPortrait) {
				//table2.addCell(ct1);
				table2.addCell(ct2);
				table2.addCell(ct3);
				if((affRangEtudiant|| afficherRangElpEpr)){
					table2.addCell(cellRang);
				}
				if(affRangEtudiant){
					table2.addCell(cellEcts);
				}
				table2.addCell(ct4);
				table2.addCell(ct5);
				table2.addCell(ct6);
				table2.addCell(ct7);
			} else {

				//table2.addCell(ct1);
				table2.addCell(ct2);
				table2.addCell(ct3);
				table2.addCell(ct4);
				table2.addCell(ct5);
				table2.addCell(ct6);
				table2.addCell(ct7);
				if((affRangEtudiant|| afficherRangElpEpr)){
					table2.addCell(cellRang);
				}
				if(affRangEtudiant){
					table2.addCell(cellEcts);
				}
			}

			for (int i = 0; i < etudiant.getElementsPedagogiques().size(); i++) {

				Paragraph pa2 = new Paragraph(etudiant.getElementsPedagogiques().get(i).getCode(), normal);
				PdfPCell celltext2 = new PdfPCell(pa2);
				celltext2.setBorder(Rectangle.NO_BORDER);
				table2.addCell(celltext2);


				String indentation = "";
				for(int j=0;j<etudiant.getElementsPedagogiques().get(i).getLevel();j++){
					indentation= indentation + "     ";
				}
				Paragraph pa3 = new Paragraph(indentation+etudiant.getElementsPedagogiques().get(i).getLibelle(), normal);
				PdfPCell celltext3 = new PdfPCell(pa3);
				celltext3.setBorder(Rectangle.NO_BORDER);
				table2.addCell(celltext3);


				if (!formatPortrait) {
					Paragraph pa5 = new Paragraph(getNote1(etudiant.getElementsPedagogiques().get(i)), normal);
					PdfPCell celltext5 = new PdfPCell(pa5);
					celltext5.setBorder(Rectangle.NO_BORDER);
					table2.addCell(celltext5);


					Paragraph pa6 = new Paragraph(etudiant.getElementsPedagogiques().get(i).getRes1(), normal);
					PdfPCell celltext6 = new PdfPCell(pa6);
					celltext6.setBorder(Rectangle.NO_BORDER);
					table2.addCell(celltext6);


					Paragraph pa7 = new Paragraph(getNote2(etudiant.getElementsPedagogiques().get(i)), normal);
					PdfPCell celltext7 = new PdfPCell(pa7);
					celltext7.setBorder(Rectangle.NO_BORDER);
					table2.addCell(celltext7);

					Paragraph pa8 = new Paragraph(etudiant.getElementsPedagogiques().get(i).getRes2(), normal);
					PdfPCell celltext8 = new PdfPCell(pa8);
					celltext8.setBorder(Rectangle.NO_BORDER);
					table2.addCell(celltext8);
				}

				if((affRangEtudiant || afficherRangElpEpr)){
					Paragraph parRang2 = new Paragraph(etudiant.getElementsPedagogiques().get(i).getRang(), normal);
					PdfPCell cellRang2 = new PdfPCell(parRang2);
					cellRang2.setBorder(Rectangle.NO_BORDER);
					table2.addCell(cellRang2);
				}

				if(affECTSEtudiant){
					Paragraph parEcts2 = new Paragraph(etudiant.getElementsPedagogiques().get(i).getEcts(), normal);
					PdfPCell cellEcts2 = new PdfPCell(parEcts2);
					cellEcts2.setBorder(Rectangle.NO_BORDER);
					table2.addCell(cellEcts2);
				}

				if (formatPortrait){
					Paragraph pa5 = new Paragraph(getNote1(etudiant.getElementsPedagogiques().get(i)), normal);
					PdfPCell celltext5 = new PdfPCell(pa5);
					celltext5.setBorder(Rectangle.NO_BORDER);
					table2.addCell(celltext5);

					Paragraph pa6 = new Paragraph(etudiant.getElementsPedagogiques().get(i).getRes1(), normal);
					PdfPCell celltext6 = new PdfPCell(pa6);
					celltext6.setBorder(Rectangle.NO_BORDER);
					table2.addCell(celltext6);

					Paragraph pa7 = new Paragraph(getNote2(etudiant.getElementsPedagogiques().get(i)), normal);
					PdfPCell celltext7 = new PdfPCell(pa7);
					celltext7.setBorder(Rectangle.NO_BORDER);
					table2.addCell(celltext7);

					Paragraph pa8 = new Paragraph(etudiant.getElementsPedagogiques().get(i).getRes2(), normal);
					PdfPCell celltext8 = new PdfPCell(pa8);
					celltext8.setBorder(Rectangle.NO_BORDER);
					table2.addCell(celltext8);
				}

			}


			document.add(table);
			document.add(table2);
			document.add(new Paragraph("\n"));


			//Partie QUESTIONS
			if(etudiant.isSignificationResultatsUtilisee()) {
				PdfPTable tablequestions = new PdfPTable(1);
				tablequestions.setWidthPercentage(98);
				PdfPCell cellquestions = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.questions", null, Locale.getDefault()).toUpperCase(Locale.getDefault()) + " ", header));
				cellquestions.setBorder(Rectangle.NO_BORDER);
				cellquestions.setBackgroundColor(headerColor);
				tablequestions.addCell(cellquestions);

				PdfPTable tablequestions2 = new PdfPTable(1);
				tablequestions2.setWidthPercentage(98);

				String grilleSignficationResultats = "";
				Set<String> ss = etudiant.getSignificationResultats().keySet();
				for(String k : ss){
					if(k != null && !k.equals("") && !k.equals(" ")){
						grilleSignficationResultats = grilleSignficationResultats + k+" : "+ etudiant.getSignificationResultats().get(k);
						grilleSignficationResultats = grilleSignficationResultats + "   ";
					}
				}

				PdfPCell cellquestions2 = new PdfPCell(new Paragraph(applicationContext.getMessage("pdf.code.resultat.signification", null, Locale.getDefault()) + " : \n" + grilleSignficationResultats, normal));
				cellquestions2.setBorder(Rectangle.NO_BORDER);
				tablequestions2.addCell(cellquestions2);

				document.add(tablequestions);
				document.add(tablequestions2);



			}

		} catch (BadElementException e) {
			LOG.error("Erreur à la génération du detail des notes : BadElementException ",e);
		} catch (MalformedURLException e) {
			LOG.error("Erreur à la génération du detail des notes : MalformedURLException ",e);
		} catch (IOException e) {
			LOG.error("Erreur à la génération du detail des notes : IOException ",e);
		} catch (DocumentException e) {
			LOG.error("Erreur à la génération du detail des notes : DocumentException ", e);
		}
		// step 6: fermeture du document.
		document.close();






	}

	private String getNote1(ElementPedagogique el) {
		String note = el.getNote1();
		if(el.getBareme1()!=0 && (configController.isToujoursAfficherBareme() || el.getBareme1()!=20)){
			note += "/"+el.getBareme1();
		}
		return note;
	}

	private String getNote2(ElementPedagogique el) {
		String note = el.getNote2();
		if(el.getBareme2()!=0 && (configController.isToujoursAfficherBareme() || el.getBareme2()!=20)){
			note += "/"+el.getBareme2();
		}
		return note;
	}

	private PdfPCell makeCell(String str, Font font) {
		PdfPCell cell = new PdfPCell();
		cell.setBorder(0);
		cell.setPhrase(new Phrase(str, font));
		return cell;
	}

	private PdfPCell makeCellSignataire(String str, Font font) {
		PdfPCell cell = makeCell(str, font);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		return cell;
	}

	
	private String getCodeSignataire(Etape et,Etudiant etudiant){

		String codSign=null;

		//récupération de la source des résultats
		String sourceResultat = PropertyUtils.getSourceResultats();
		if(sourceResultat == null || sourceResultat.equals("")){
			sourceResultat="Apogee";
		}
		

		//Si on doit se baser sur l'extraction Apogée
		if(etudiantController.utilisationExtractionApogee(et.getAnnee().substring(0, 4),sourceResultat)){
			//On se base sur l'extraction apogée
			sourceResultat="Apogee-extraction";
		}else{
			//On va chercher les résultats directement dans Apogée
			sourceResultat="Apogee";
		}

		// si sourceResultat = apogee-extraction
		if(sourceResultat.compareTo("Apogee-extraction")==0){
			//Si on doit aller chercher une signature à apposer sur le document
			if(configController.isNotesPDFsignature()){
				// on teste s'il y a bien des elps presents
				if (etudiant.getElementsPedagogiques().size()>1){
					// PFE : on teste si on a un relevé de notes (extraction) associé à l'élément pédagogique
					List<BigDecimal> CodRvn = multipleApogeeService.getCodRvn(etudiant.getCod_ind(), etudiant.getElementsPedagogiques().get(0).getAnnee().substring(0, 4), getListeCodElpForSQL(etudiant.getElementsPedagogiques()));
					if (!CodRvn.isEmpty()) {
						codSign = multipleApogeeService.getCodSignataireRvn(CodRvn.get(0));
					}
				}
			}
		}
		return codSign;
	}

	/**
	 * 
	 * @param elementsPedagogiques
	 * @return la liste des codeElp concaténés avec des simples quotes et séparés par des virgules
	 */
	private String getListeCodElpForSQL(List<ElementPedagogique> elementsPedagogiques) {
		String listeCodes="";
		for(ElementPedagogique e : elementsPedagogiques){
			if(StringUtils.hasText(listeCodes))
				listeCodes += ",";
			listeCodes += "'"+e.getCode()+"'";
		}
		return listeCodes;
	}

	/**
	 * Inner class to add a watermark to every page.
	 */
	class Watermark extends PdfPageEventHelper {

		/** Default watermark font */
		Font FONT = new Font(5, 52, Font.BOLD, new GrayColor(0.75f));
		boolean formatPortrait;
		float coordonneex;
		float coordonneey;
		public Watermark(boolean notesPDFFormatPortrait) {
			formatPortrait = notesPDFFormatPortrait;
			if(formatPortrait){
				coordonneex=295;
				coordonneey=400.5f;
			}else{
				coordonneex=421;
				coordonneey=297.5f;
			}
		}
		@Override
		public void onEndPage(PdfWriter writer, Document document) {
			ColumnText.showTextAligned(
					writer.getDirectContentUnder(),
					Element.ALIGN_CENTER,
					new Phrase(applicationContext.getMessage("pdf.filigrane", null, Locale.getDefault()).toUpperCase(), FONT),
					coordonneex, coordonneey,
					writer.getPageNumber() % 2 == 1 ? 45 : -45);
		}
	}
}
