/*****************************************************************************
 * All public interface based on Starteam API are a property of Borland, 
 * those interface are reproduced here only for testing purpose. You should
 * never use those interface to create a competitive product to the Starteam
 * Server. 
 * 
 * The implementation is given AS-IS and should not be considered a reference 
 * to the API. The behavior on a lots of method and class will not be the
 * same as the real API. The reproduction only seek to mimic some basic 
 * operation. You will not found anything here that can be deduced by using
 * the real API.
 * 
 * Fake-Starteam is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *****************************************************************************/
package com.starbase.starteam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.ossnoize.fakestarteam.FileUtility;
import org.ossnoize.fakestarteam.InternalPropertiesProvider;
import org.ossnoize.fakestarteam.SimpleTypedResourceIDProvider;
import org.ossnoize.fakestarteam.exception.InvalidOperationException;

public class Folder extends Item {
	private static final String FOLDER_PROPERTIES = "folder.properties";
	private static final FilenameFilter FOLDER_TESTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.equalsIgnoreCase(FOLDER_PROPERTIES);
		}
	};

	private static final FilenameFilter FILE_TESTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean valid = true;
			for(char c : name.toCharArray()) {
				if(!Character.isDigit(c)) {
					valid = false;
				}
			}
			return valid;
		}
	};

	public Folder(Server server) {
		throw new UnsupportedOperationException("Unknown goal for this constructor");
	}
	
	public Folder(Folder parent, String name, String workingFolder) {
		this.parent = parent;
		view = parent.getView();
		try {
			String folder = parent.holdingPlace.getCanonicalPath() + File.separator + name;
			holdingPlace = new File(folder);
			validateHoldingPlace();
			loadFolderProperties();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected Folder(View currentView) {
		File serverArchive = InternalPropertiesProvider.getInstance().getFile();
		if(!serverArchive.isDirectory()) {
			throw new UnsupportedOperationException("The archive need to be a directory.");
		}
		try {
			String rootFolder = serverArchive.getCanonicalPath() + File.separator +
					currentView.getProject().getName() + File.separator + currentView.getName();
			holdingPlace = new File(rootFolder);
			validateHoldingPlace();
			loadFolderProperties();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// We don't want the root folder to have any default name.
		// so overwrite the view name with a blank one and update.
		setName("");
		this.parent = null;
		view = currentView;
		update();
	}

	private void validateHoldingPlace() {
		if(null == holdingPlace) {
			throw new InvalidOperationException("Cannot create a folder without an holding place.");
		}
		if(holdingPlace.exists()) {
			if(holdingPlace.isFile()) {
				holdingPlace.delete();
				holdingPlace.mkdirs();
			}
		} else {
			holdingPlace.mkdirs();
		}
	}
	
	public void setName(java.lang.String name) {
		if(itemProperties == null) {
			throw new InvalidOperationException("The properties are not initialized");
		}
		itemProperties.setProperty(propertyKeys.FOLDER_NAME, name);
	}
	
	public String getName() {
		if(itemProperties == null) {
			throw new InvalidOperationException("The properties are not initialized");
		}
		return itemProperties.getProperty(propertyKeys.FOLDER_NAME);
	}
	
	public Folder[] getSubFolders() {
		List<Folder> generatedList = new ArrayList<Folder>();
		for(File f : holdingPlace.listFiles()) {
			if(f.isDirectory()) {
				String[] subFolders = f.list(FOLDER_TESTER);
				if(subFolders != null && subFolders.length == 1) {
					generatedList.add(new Folder(this, f.getName(), ""));
				}
			}
		}
		Folder[] buffer = new Folder[generatedList.size()];
		return generatedList.toArray(buffer);
	}
	
	private com.starbase.starteam.File[] getFiles() {
		List<com.starbase.starteam.File> generatedList = new ArrayList<com.starbase.starteam.File>();
		for(File f : holdingPlace.listFiles()) {
			if(f.isDirectory()) {
				String[] fileRevision = f.list(FILE_TESTER);
				if(null != fileRevision && fileRevision.length >= 1) {
					generatedList.add(new com.starbase.starteam.File(f.getName(), this));
				}
			}
		}
		com.starbase.starteam.File[] buffer = new com.starbase.starteam.File[generatedList.size()];
		return generatedList.toArray(buffer);
	}
	
	public Item[] getItems(java.lang.String typeName) {
		if(typeName.equalsIgnoreCase(getTypeNames().FOLDER))
			return getSubFolders();
		else if (typeName.equalsIgnoreCase(getTypeNames().FILE)) {
			return getFiles();
		}
		return new Item[0];
	}
	
	@Override
	public Folder getParentFolder() {
		return parent;
	}
	
	@Override
	public void update() {
		if(itemProperties == null) {
			throw new InvalidOperationException("Properties are not initialized yet!!!");
		}
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(holdingPlace.getCanonicalPath() + File.separator + FOLDER_PROPERTIES);
			itemProperties.store(fout, "Folders properties");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			FileUtility.close(fout);
		}
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	private void loadFolderProperties() {
		itemProperties = new Properties();
		FileInputStream fin = null;
		try {
			File folderProperty = new File(holdingPlace.getCanonicalPath() + File.separator + FOLDER_PROPERTIES);
			if(folderProperty.exists()) {
				fin = new FileInputStream(folderProperty);
				itemProperties.load(fin);
				int id = Integer.parseInt(itemProperties.getProperty(propertyKeys.OBJECT_ID));
				SimpleTypedResourceIDProvider.getProvider().registerExisting(id, this);
			} else {
				// initialize the basic properties of the folder.
				itemProperties.setProperty(propertyKeys.OBJECT_ID, 
						Integer.toString(SimpleTypedResourceIDProvider.getProvider().registerNew(this)));
				setName(holdingPlace.getName());
				update();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			FileUtility.close(fin);
		}
	}
}