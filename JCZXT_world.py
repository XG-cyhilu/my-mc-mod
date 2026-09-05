import tkinter as tk
import importlib.util
from pathlib import Path
from tkinter import messagebox


class WorldApp:
	def __init__(self, root):
		self.root = root
		self.easy_window = None
		root.title("JCZXT 世界")
		root.geometry("700x560")
		root.minsize(560, 420)
		root.configure(bg="#f3f5f7")

		header = tk.Frame(root, bg="#202b36")
		header.pack(fill="x")
		tk.Label(
			header,
			text="JCZXT世界",
			bg="#202b36",
			fg="white",
			font=("Microsoft YaHei UI", 20, "bold"),
		).pack(pady=14)

		self.content = tk.Frame(root, bg="#f3f5f7")
		self.content.pack(fill="both", expand=True, anchor="nw", padx=28, pady=24)
		self.create_option("简单", self.open_easy)
		for difficulty in ("普通", "困难", "噩梦"):
			self.create_option(difficulty, self.not_available)

	def create_option(self, difficulty, command):
		option_frame = tk.Frame(
			self.content,
			bg="#f3f5f7",
			highlightbackground="#111111",
			highlightcolor="#111111",
			highlightthickness=2,
			width=260,
			height=58,
		)
		option_frame.pack(anchor="w", pady=8)
		option_frame.pack_propagate(False)
		tk.Button(
			option_frame,
			text=difficulty,
			command=lambda: command(difficulty),
			bg="#f3f5f7",
			fg="#111111",
			activebackground="#dce2e7",
			activeforeground="#111111",
			relief="flat",
			bd=0,
			font=("Microsoft YaHei UI", 15),
		).pack(fill="both", expand=True)

	def not_available(self, difficulty):
		messagebox.showinfo("提示", f"“{difficulty}”世界还在开发中。")

	def open_easy(self, _difficulty):
		if self.easy_window is not None and self.easy_window.winfo_exists():
			self.easy_window.lift()
			return
		easy_path = Path(__file__).with_name("JCZXT_world_easy.py")
		easy_spec = importlib.util.spec_from_file_location("JCZXT_world_easy", easy_path)
		easy_module = importlib.util.module_from_spec(easy_spec)
		easy_spec.loader.exec_module(easy_module)
		self.easy_window = tk.Toplevel(self.root)
		easy_module.EasyWorldApp(self.easy_window)
		self.easy_window.protocol("WM_DELETE_WINDOW", self.close_easy)

	def close_easy(self):
		if self.easy_window is not None:
			self.easy_window.destroy()
			self.easy_window = None


if __name__ == "__main__":
	window = tk.Tk()
	WorldApp(window)
	window.mainloop()
