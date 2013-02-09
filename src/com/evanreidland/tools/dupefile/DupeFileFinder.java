package com.evanreidland.tools.dupefile;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class DupeFileFinder extends JPanel implements ActionListener
{
	private static final long serialVersionUID = -3862205528056661463L;

	JFrame					frame;
	
	JButton					begin,
							newSearch;
	
	JTextField				dirField,
							stats;
	
	boolean					searching;
	
	DataInputStream 		inf;
	PrintStream				log;
	
	Queue<File>				dirs;
	HashMap<String,
		Vector<String>>		hashMap;
	
	String					baseDir;
	
	int						fileCount,
							dirCount,
							dupeCount;
	
	Thread					searchThread;
	
	void endSearch()
	{
		searchThread = null;
		searching = false;
		begin.setText("Begin");
	}
	
	String hashFile(File file)
	{
		try
		{
			InputStream in = null;
			MessageDigest md = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			in = new DigestInputStream(in, md);
			in.close();
			return String.valueOf(md.digest());
		}
		catch (Exception e) { e.printStackTrace(); }
		return null;
	}
	
	void iterateSearch(File dir)
	{
		File[] all = dir.listFiles();
		for (File f : all)
		{
			stats.setText("Scanning " + f.getAbsolutePath());
			if (f.isDirectory())
			{
				dirCount++;
				dirs.push(f);
			}
			else
			{
				String res = hashFile(f);
				if (res != null)
				{
					if (hashMap.containsKey(res))
					{
						hashMap.get(res).add(f.getAbsolutePath());
						dupeCount++;
					}
					else
					{
						Vector<String> v = new Vector<String>();
						v.add(f.getAbsolutePath());
						hashMap.put(res, v);
					}
				}
				fileCount++;
			}
			
			stats.setText("Files: " + fileCount + " Folders: " + dirCount + " Dupes: " + dupeCount);
		}
	}
	
	void runSearch()
	{
		dirs = new Queue<File>();
		hashMap = new HashMap<String, Vector<String>>();
		dupeCount = fileCount = dirCount = 0;
		File base = new File(baseDir);
		if (base.isDirectory())
		{
			stats.setText("Beginning at: " + baseDir);
			iterateSearch(base);
			while(!dirs.empty())
			{
				File dir = dirs.pop();
				iterateSearch(dir);
			}
			endSearch();
		}
		else
		{
			stats.setText("Error: \"" + baseDir + "\" is not a valid directory.");
			endSearch();
		}
	}
	
	void launch()
	{
		frame = new JFrame();
		frame.setTitle("Evan's Duplicate File Finder");
		frame.setSize(520, 128);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.add(this);
		//setLayout(new FlowLayout());
		
		dirField = new JTextField();
		dirField.setPreferredSize(new Dimension(512, 24));
		dirField.addActionListener(this);
		add(dirField);
		
		newSearch = new JButton("New Search");
		newSearch.addActionListener(this);
		add(newSearch);
		
		begin = new JButton("Begin");
		begin.setEnabled(false);
		begin.addActionListener(this);
		add(begin);
		
		stats = new JTextField("No searches yet.");
		stats.setPreferredSize(new Dimension(512, 24));
		stats.setEditable(false);
		add(stats);
		
		frame.setVisible(true);
		
		searchThread = null;
	}
	
	void primeSearch()
	{
		begin.setEnabled(true);
	}
	
	void beginSearch()
	{
		if (!searching)
		{
			begin.setText("Stop");
			searching = true;
			dirField.setText(dirField.getText().replace('\\', '/'));
			baseDir = dirField.getText();
			
			searchThread = new Thread(new Runnable() { public void run() { runSearch(); } });
			searchThread.start();
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent act)
	{
		if (act.getSource() == newSearch)
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("."));
			chooser.setDialogTitle("Pick a folder");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			
			if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				baseDir = chooser.getCurrentDirectory().getAbsolutePath();
				dirField.setText(baseDir);
				primeSearch();
			}
		}
		else if (act.getSource() == dirField)
			primeSearch();
		else if (act.getSource() == begin)
		{
			if (searching)
				endSearch();
			else
				beginSearch();
		}
	}
	
	public static void main(String[] args)
	{
		DupeFileFinder app = new DupeFileFinder();
		app.launch();
	}
}
