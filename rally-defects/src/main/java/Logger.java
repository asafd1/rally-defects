import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger
{
	final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	PrintStream log;

	public Logger(String file)
	{
		try
		{
			log = new PrintStream(new FileOutputStream(file));
		}
		catch (FileNotFoundException e)
		{
			System.out.println("no log");
		}
	}

	public void println(String msg)
	{
		msg = time() + " --> " + msg;
		if (log != null)
		{
			log.println(msg);
		}
		System.out.println(msg);
	}

	public void print(String msg)
	{
		msg = time() + " --> " + msg;
		if (log != null)
		{
			log.print(msg);
		}
		System.out.print(msg);
	}

	public void printRaw(String msg)
	{
		if (log != null)
		{
			log.println(msg);
		}
		System.out.println(msg);
	}

	private String time()
	{
		return dateFormat.format(new Date(System.currentTimeMillis()));
	}

}
