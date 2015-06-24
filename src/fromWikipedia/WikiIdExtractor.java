package fromWikipedia;

import basics.Fact;
import basics.FactComponent;
import extractors.MultilingualWikipediaExtractor;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import fromOtherSources.PatternHardExtractor;
import fromOtherSources.WordnetExtractor;
import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.MultilingualTheme;
import utils.Theme;
import utils.TitleExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Extracts all Wikipedia IDs.
 * 
 * @author Johannes Hoffart
 * 
 */
public class WikiIdExtractor extends MultilingualWikipediaExtractor {

	@Override
	public Set<Theme> input() {
    return new FinalSet<Theme>(PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.PREFMEANINGS, PatternHardExtractor.LANGUAGECODEMAPPING);
	}

	public static final MultilingualTheme WIKIPEDIAIDFACTSNEEDSTRANSLATION = new MultilingualTheme(
			"wikipediaIdNeedsTranslation",
			"Extracted IDs from Wikipedia articles pages (to be translated)");

	public static final MultilingualTheme WIKIPEDIAIDFACTS = new MultilingualTheme(
			"wikipediaIds",
			"Extracted IDs from Wikipedia articles pages");

	@Override
	public Set<FollowUpExtractor> followUp() {
		HashSet<FollowUpExtractor> s=new HashSet<>();
		if (!isEnglish()) {
		  s.add(new EntityTranslator(WIKIPEDIAIDFACTSNEEDSTRANSLATION.inLanguage(language), WIKIPEDIAIDFACTS.inLanguage(language), this));
		}
		return s;
	}

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(
				WIKIPEDIAIDFACTSNEEDSTRANSLATION.inLanguage(this.language));
	}

	@Override
	public void extract() throws Exception {
		Announce.doing("Extracting Wikipedia IDs");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);
		TitleExtractor titleExtractor = new TitleExtractor(language);
		Theme out = WIKIPEDIAIDFACTSNEEDSTRANSLATION.inLanguage(this.language);

		String titleEntity = null;
		redirect: while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>", "<id>")) {
			case -1:
				Announce.done();
				in.close();
				break redirect;
			case 0:
				titleEntity = titleExtractor.getTitleEntity(in);
				break;
			case 1:
				if (titleEntity == null)
					continue;
				Integer id = Integer.parseInt(FileLines.readTo(in, "</id>").toString());
        // Make sure to take only the first id encountered after the title. Other ids might be revisions etc.
				titleEntity = null;
				out.write(new Fact(
						titleEntity, "<hasWikipediaId>",
						FactComponent.forNumber(id))
				);
			}
		}

		Announce.done();
	}

	public WikiIdExtractor(String lang, File wikipedia) {
		super(lang, wikipedia);
	}

	public static void main(String[] args) throws Exception {
		Announce.setLevel(Announce.Level.DEBUG);
		new WikiIdExtractor("en", new File("D:/en_wikitest.xml")).extract(
				new File("D:/data3/yago2s"), "Test on 1 wikipedia article");
	}
}
