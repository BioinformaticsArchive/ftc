package build;

import groovy.lang.Tuple;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.FtcClass;

import play.Logger;
import uk.ac.ebi.brain.core.Brain;
import uk.ac.ebi.brain.error.BadPrefixException;
import uk.ac.ebi.brain.error.BrainException;
import uk.ac.ebi.brain.error.ClassExpressionException;
import uk.ac.ebi.brain.error.NewOntologyException;
import uk.ac.ebi.brain.error.NonExistingEntityException;
import utils.DotRelation;
import utils.DotRelations;
import utils.GraphViz;

public class DatabaseFiller {

	private String pathToKb;
	public final static String LOCATION_GRAPHS = "public/images/graphs/";
	public static int MAX_WIDTH;


	public DatabaseFiller(String pathToOwlFile) {
		this.setPathToKb(pathToOwlFile);
	}

	public String getPathToKb() {
		return pathToKb;
	}

	public void setPathToKb(String pathToKb) {
		this.pathToKb = pathToKb;
	}

	public void start() throws BrainException, IOException {
		Logger.info("Learning the KB...");
		Brain brain = new Brain();
		brain.learn(this.getPathToKb());
		Logger.info("Getting the therapeutic agents...");

		//FTC_C1 - only the one I've created are interesting :-P
		//TODO: Put the FTC_C1 class instead of the current one for dev
		List<String> ftcAndDrugBankClasses = brain.getSubClasses("FTC_C1", false);
//		List<String> ftcAndDrugBankClasses = brain.getSubClasses("FTC_A0050817", false);
		List<String> drugBankClasses = brain.getSubClasses("FTC_C2", false);
		List<String> ftcClasses = new ArrayList<String>();

		//Retrieves only the FTC classes and not the drugBank ones
		for (String ftcOrDrugBankClass : ftcAndDrugBankClasses) {
			if(!drugBankClasses.contains(ftcOrDrugBankClass)){
				ftcClasses.add(ftcOrDrugBankClass);
			}
		}

		Logger.info("There are a total of: " + ftcClasses.size() + " agents");
		int total = ftcClasses.size();
		int counter = 1;

		//Foreach FTC class, get the suclasses, id, etc... and generates the SVG graph
		for (String ftcClass : ftcClasses) {
			Logger.info("Storing class " + ftcClass + " in database - " + counter+ "/" + total);
			counter++;
			String label = brain.getLabel(ftcClass);
			String ftcId = ftcClass;
			//TODO: get the rdfs:comment + other info if needed
			//TODO implement the superclasses
			//						List<String> superClasses = brain.getSuperClasses(ftcClass, true);
			List<String> subClasses = brain.getSubClasses(ftcClass, true);
			subClasses.removeAll(drugBankClasses);

			//Save the graph as SVG to be ready to be rendered.
			//Returns the size of the graph that will be used later on to adjust on the browser.
			int width = saveGraph(brain, ftcClass);

			//Keep the maximal width in order to define the maximal dimension of the SVG graphs
			//TODO: max width shouldn't be handled in memory but put directly in the JPA entity
			//TODO: tireate over all FTCclass and update the value
			MAX_WIDTH = 0;
			if(width > MAX_WIDTH){
				MAX_WIDTH = width;
				Logger.info("Max width: " + ftcId);
			}

			//Create a new JPA entity with values used for the rendering later on.
			new FtcClass(ftcId, label, subClasses, width).save();

		}
		Logger.info("Max width: " + MAX_WIDTH);
		brain.sleep();

	}


	private int saveGraph(Brain brain, String ftcClass) throws BrainException, IOException {

		GraphViz gv = new GraphViz();
		gv.addln(gv.start_graph());
		//Initialize the layout of the SVG graph
		gv.addln("graph [splines=true overlap=false rankdir=BT nodesep=0.1 ranksep=0.2];");
		gv.addln("node [shape=box style = filled color=springgreen fixedsize=true width=2 height=0.25 fontsize=6];");
		gv.addln("edge [arrowsize=0.3 color=gray];");

		//Maintain the id of the class visited in order to not display edges multiple times.
		DotRelations alreadyVisited = new DotRelations();
		//Classes not informative, not to be displayed - Arbitrary value.
		List<String> undesirableClasses = brain.getSuperClasses("FTC_C1", false);

		//Recursive function: Fill the gv object with the relations between class.
		addSuperClasses(ftcClass, gv, brain, alreadyVisited, undesirableClasses);

		List<String> drugBankClasses = brain.getSubClasses("FTC_C2", false);
		addSubClasses(ftcClass, gv, brain, alreadyVisited, undesirableClasses, drugBankClasses);

		//Once all the relations are known, adds URLs to nodes.
		for (String node : alreadyVisited.getAllNodesOnce()) {
			//TODO: put the good URL
			gv.addln(node + " [URL=<http://localhost:9000/"+ node +">];");
			gv.addln(node + " [label=\"\\N\\n" + brain.getLabel(node) + "\"];");
			if(ftcClass.equals(node)){
				gv.addln(node + " [fillcolor=\"#00ece4\"];");
			}
		}

		gv.addln(gv.end_graph());
		String type = "svg";
		//Save the SVG graph. Arbitrary location
		String pathSvgFile = LOCATION_GRAPHS + ftcClass + "." + type;
		File out = new File(pathSvgFile);
		gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );

		//Block to hack the SVG content and replace it in an Web friendly way
		String svgContent = play.vfs.VirtualFile.fromRelativePath(pathSvgFile).contentAsString();
		String withoutXlinkSvgContent = svgContent.replaceAll("xlink:href", "target='_top' xlink:href")
				.replaceAll("<svg.*\n", "<svg");

		//Get the with of the SVG. Used later to render the SVG correctly on the browser
		Pattern pattern = Pattern.compile("viewBox=\"\\d+\\.\\d\\d \\d+\\.\\d\\d (\\d+)\\.\\d\\d \\d+\\.\\d\\d\"");
		Matcher matcher = pattern.matcher(withoutXlinkSvgContent);
		int width = 0;
		while (matcher.find()) {
			width = Integer.parseInt(matcher.group(1));
		}

		//Write the corrected file
		FileWriter fout = new FileWriter(out);		
		fout.write(withoutXlinkSvgContent);
		fout.close();
		return width;

	}

	//Add the direct subclasses to the graph
	private void addSubClasses(String ftcClass, GraphViz gv, Brain brain, DotRelations alreadyVisited, List<String> undesirableClasses, List<String> drugBankClasses) throws BrainException {
		List<String> directSubClasses = brain.getSubClasses(ftcClass, true);
		directSubClasses.removeAll(undesirableClasses);
		directSubClasses.removeAll(drugBankClasses);
		for (String directSubClass : directSubClasses) {

			if(!alreadyVisited.contains(directSubClass, ftcClass)){
				gv.addln(directSubClass + " -> " + ftcClass + ";");
				alreadyVisited.addRelation(directSubClass, ftcClass);
			}
		}
	}

	//Adds the relations between classes for the graph
	private void addSuperClasses(String ftcClass, GraphViz gv, Brain brain, DotRelations alreadyVisited, List<String> undesirableClasses) throws BrainException {
		List<String> directSuperClasses = brain.getSuperClasses(ftcClass, true);
		directSuperClasses.removeAll(undesirableClasses);
		for (String directSuperClass : directSuperClasses) {

			if(!alreadyVisited.contains(ftcClass, directSuperClass)){
				gv.addln(ftcClass + " -> " + directSuperClass + ";");
				alreadyVisited.addRelation(ftcClass, directSuperClass);
				addSuperClasses(directSuperClass, gv, brain, alreadyVisited, undesirableClasses);
			}
		}
	}


}
