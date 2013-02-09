package com.evanreidland.tools.dupefile;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
							stats,
							scanning;
	
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
	
	long					startTime;
	
	Thread					searchThread,
							updateStatus;
	
	String[]				hexValues;
	
	String hexDigest(byte[] values)
	{
		StringBuilder str = new StringBuilder(values.length*2);
		for (int i = 0; i < values.length; i++)
			str.append(hexValues[0xFF & values[i]]);
		return str.toString();
	}
	
	void endSearch()
	{
		searchThread = null;
		searching = false;
		if (dupeCount > 0)
		{
			logEverything("log.txt");
			scanning.setText("Logged dupes to log.txt");
		}
		else
			scanning.setText("No duplicates found.");
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
			
			byte[] toread = new byte[1024];
			
			while (in.available() > 0)
				in.read(toread);
			
			String str = hexDigest(md.digest());
			
			in.close();
			return str;
		}
		catch (Exception e) { e.printStackTrace(); }
		return null;
	}
	
	String getTimeDiff(long ms)
	{
		StringBuilder time = new StringBuilder();
		if (ms > 60000)
		{
			long min = ms/60000;
			ms -= min*60000;
			time.append(min + " min ");
		}
		if (ms > 1000)
		{
			long sec = ms/1000;
			ms -= sec*1000;
			time.append(sec + "." + ms/100 + " sec" );
		}
		return time.toString();
	}
	
	void iterateSearch(File dir)
	{
		File[] all = dir.listFiles();
		if (all == null) return;
		for (File f : all)
		{
			scanning.setText(f.getAbsolutePath());
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
		}
	}
	
	void logEverything(String file)
	{
		try	{
			PrintStream str = new PrintStream(new FileOutputStream(file));
			for (Map.Entry<String, Vector<String>> dupes : hashMap.entrySet())
			{
				String hash = dupes.getKey();
				Vector<String> list = dupes.getValue();
				if (list.size() > 1)
				{
					str.println(list.size() + " matches for hash " + hash);
					for (String path : list)
						str.println("----" + path);
					str.println("----------------");
				}
			}
		} catch (Exception e) { e.printStackTrace(); }
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
			while(!dirs.empty() && searching)
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
		frame.setTitle("(evanreidland.com) Evan's Duplicate File Finder.");
		frame.setSize(520, 148);
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
		
		scanning = new JTextField();
		scanning.setPreferredSize(new Dimension(512, 24));
		scanning.setEditable(false);
		add(scanning);
		
		stats = new JTextField("No searches yet.");
		stats.setPreferredSize(new Dimension(512, 24));
		stats.setEditable(false);
		add(stats);
		
		hexValues = new String[256];
		for (int i = 0; i < 255; i++)
		{
			hexValues[i] = Integer.toHexString(i);
			if (hexValues[i].length() == 1)
				hexValues[i] = "0" + hexValues[i];
		}
		
		updateStatus = new Thread(
							new Runnable()
							{
								public void run()
								{
									while (true)
									{
										try 
										{
											Thread.sleep(100);
											if (searching)
												stats.setText("Files: " + fileCount + " Folders: " + dirCount + " Dupes: " + dupeCount + " Duration: " + getTimeDiff(System.currentTimeMillis() - startTime));
										}
										catch ( Exception e ) {}
									}
								}
							}
						);
		updateStatus.start();
		
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
			startTime = System.currentTimeMillis();
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
				baseDir = chooser.getSelectedFile().getAbsolutePath();
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
