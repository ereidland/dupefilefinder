package com.evanreidland.tools.dupefile;

public class Queue<T>
{
	private class Node
	{
		T		data;
		
		Node	next;
		
		public Node(T data)
		{
			this.data = data;
			next = null;
		}
	}
	
	private Node first, last;
	
	public void push(T elem)
	{
		if (first != null)
		{
			last.next = new Node(elem);
			last = last.next;
		}
		else
			first = last = new Node(elem);
	}
	
	public T pop()
	{
		if (first != null)
		{
			T data = first.data;
			if (first.next != null)
				first = first.next;
			else
				first = last = null;
			return data;
		}
		return null;
	}
	
	public boolean empty() { return first == null; }
	
	public Queue() { first = last = null; }
}
