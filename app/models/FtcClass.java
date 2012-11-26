package models;

import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.validation.constraints.Max;

import org.hibernate.annotations.Type;

import play.db.jpa.Blob;
import play.db.jpa.Model;

@Entity
public class FtcClass extends Model {

	public String label;
	public String ftcId;
	public int widthSvg;
	
	@Lob
	public String comment;

	@ElementCollection
	public List<String> superClasses;

	@ElementCollection
	public List<String> subClasses;

	public FtcClass(String ftcId, String label, String comment, List<String> subClasses, List<String> superClasses, int width) {
		this.ftcId = ftcId;
		this.label = label;
		this.subClasses = subClasses;
		this.widthSvg = width;
		this.superClasses = superClasses;
		this.comment = comment;
	}

}
