package toolbox.tail;

import java.io.*;

/**
 * Tails one or more files
 */
public class Main
{
	static final int NUM_LINES = 10;

	/**
	 * Entrypoint 
	 */	
	public static void main(String args[])
	{
		Main tail = new Main(args);
	}

	/**
	 * Arg constructor
	 * 
	 * @param  args  Array of files to tail
	 */
	public Main(String args[])
	{
	
		if(args.length == 0)
		{
			printUsage();
			return;
		}
			
		String files[] = args;

		for (int i = 0; i < files.length; i++)
		{
			Thread t = new Thread(new TailRunner(files[i]));
			t.start();
		}
	}

	/**
	 * Runner for each tail
	 */
	class TailRunner implements Runnable
	{
		String _filename;

		/**
		 * Creates a TailRunner
		 * 
		 * @arg  filename   Name of file to tail
		 */
		public TailRunner(String filename)
		{
			_filename = filename;
		}

		/**
		 * Runnable interface 
		 */
		public void run()
		{
			try
			{
				FileReader fr = new FileReader(_filename);
				LineNumberReader lnr = new LineNumberReader(fr);

				int cnt = 0;
				lnr.mark(1000);

				synchronized (TailRunner.class)
				{
					while (lnr.ready())
					{
						cnt++;
						if ((cnt % Main.NUM_LINES) == 0)
							lnr.mark(1000);
						lnr.readLine();
					}
				}

				lnr.reset();

				while (true)
				{
					if (lnr.ready())
					{
						String line = lnr.readLine();
						if (line != null)
							System.out.println(line);
					}
					Thread.currentThread().sleep(1);
				}
			}
			catch (Exception e)
			{
				System.out.println(e);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Program usage 
	 */
	private void printUsage()
	{
		System.out.println("Program : Tails a list of files");
		System.out.println("Usage   : java toolbox.tail.Main [file1 file2 ... file8]");
		System.out.println("Example : java toolbox.tail.Main appserver.log");
	}
}