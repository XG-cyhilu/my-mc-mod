import json
import tkinter as tk
from pathlib import Path
from tkinter import messagebox

DATA_FILE = Path(__file__).with_name("JCZXT_M.json")
BAG_FILE = Path(__file__).with_name("JCZXT_bag_thing.json")
MAX_DETECTABLE_M = 1048576

DEFAULT_DATA = {
    "M币": 1000,
    "物品": {
        "近战": [
            {"名称": "木棍", "伤害": 1, "距离": 1, "价格": 1}, {"名称": "砖头", "伤害": 2, "距离": 1, "价格": 3},
            {"名称": "小刀", "伤害": 5, "距离": 1, "价格": 10}, {"名称": "匕首", "伤害": 6, "距离": 1, "价格": 15},
            {"名称": "砍刀", "伤害": 10, "距离": 2, "价格": 25}, {"名称": "长刀", "伤害": 12, "距离": 2, "价格": 30},
            {"名称": "剑", "伤害": 15, "距离": 2, "价格": 45}, {"名称": "长剑", "伤害": 15, "距离": 3, "价格": 50},
            {"名称": "帝国之刃", "伤害": 20, "距离": 3, "价格": 100}, {"名称": "帝国之剑", "伤害": 30, "距离": 3, "价格": 120},
            {"名称": "帝国光辉之刃", "伤害": 50, "距离": 3, "价格": 200}, {"名称": "帝国光辉之剑", "伤害": 80, "距离": 3, "价格": 300},
            {"名称": "荣光", "伤害": 180, "距离": 5, "价格": 1000, "简介": "荣光？这代表了什么？"},
        ],
        "远程": [
            {"名称": "手枪", "伤害": 5, "距离": 5, "价格": 25}, {"名称": "增强手枪", "伤害": 6, "距离": 8, "价格": 35},
            {"名称": "手枪子弹*10", "价格": 10, "简介": "用于手枪和增强手枪的子弹"}, {"名称": "步枪", "伤害": 8, "距离": 15, "价格": 60},
            {"名称": "增强步枪", "伤害": 10, "距离": 18, "价格": 80}, {"名称": "步枪子弹*8", "价格": 20, "简介": "用于步枪和增强步枪的子弹"},
            {"名称": "狙击枪", "伤害": 15, "距离": 25, "价格": 120}, {"名称": "狙击枪子弹*5", "价格": 40, "简介": "用于狙击枪的子弹"},
            {"名称": "帝国之光", "伤害": 50, "距离": 40, "价格": 750, "简介": "光？为何这么说呢？"}, {"名称": "传承", "伤害": 200, "距离": 80, "价格": 2000, "简介": "传承吗"},
        ],
        "医疗": [
            {"名称": "创可贴", "伤害": -5, "距离": 0, "价格": 20, "简介": "只能对自己使用"}, {"名称": "绷带", "伤害": -8, "距离": 0, "价格": 40},
            {"名称": "药品", "伤害": -15, "距离": 0, "价格": 80}, {"名称": "神奇的药品", "伤害": -50, "距离": 0, "价格": 350}, {"名称": "辉", "伤害": -150, "距离": 0, "价格": 1200,"简介":"辉？是余晖？不、是光辉"},
        ],
    },
}


def load_data():
    if not DATA_FILE.exists():
        DATA_FILE.write_text(json.dumps(DEFAULT_DATA, ensure_ascii=False, indent=2), encoding="utf-8")
    try:
        data = json.loads(DATA_FILE.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            return DEFAULT_DATA.copy()
        data.setdefault("M币", DEFAULT_DATA["M币"])
        data.setdefault("物品", DEFAULT_DATA["物品"])
        return data
    except (json.JSONDecodeError, OSError):
        return DEFAULT_DATA.copy()


def add_to_bag(item_name):
    try:
        bag = json.loads(BAG_FILE.read_text(encoding="utf-8")) if BAG_FILE.exists() else {}
        bag[item_name] = bag.get(item_name, 0) + 1
        BAG_FILE.write_text(json.dumps(bag, ensure_ascii=False, indent=2), encoding="utf-8")
    except (json.JSONDecodeError, OSError):
        BAG_FILE.write_text(json.dumps({item_name: 1}, ensure_ascii=False, indent=2), encoding="utf-8")


class ShopApp:
    def __init__(self, root):
        self.root = root
        self.data = load_data()
        if self.data.get("M币", 0) > MAX_DETECTABLE_M:
            messagebox.showerror("商店不可用", "M币无法检测，暂时不能使用商店。")
            root.destroy()
            return
        root.title("JCZXT 商店")
        root.geometry("900x620")
        root.minsize(700, 480)
        root.configure(bg="#f3f5f7")
        top = tk.Frame(root, bg="#202b36")
        top.pack(fill="x")
        tk.Label(top, text="JCZXT 商店", fg="white", bg="#202b36", font=("Microsoft YaHei UI", 20, "bold")).pack(side="left", padx=24, pady=14)
        self.balance_label = tk.Label(top, fg="#ffd166", bg="#202b36", font=("Microsoft YaHei UI", 14, "bold"))
        self.balance_label.pack(side="right", padx=24)
        self.category_bar = tk.Frame(root, bg="white")
        self.category_bar.pack(fill="x")
        self.content = tk.Frame(root, bg="#f3f5f7")
        self.content.pack(fill="both", expand=True, padx=20, pady=18)
        self.show_category(next(iter(self.data["物品"])))

    def update_balance(self):
        self.balance_label.config(text=f"M币：{self.data['M币']}")

    def show_category(self, category):
        for widget in self.category_bar.winfo_children():
            widget.destroy()
        for name in self.data["物品"]:
            tk.Button(self.category_bar, text=name, command=lambda value=name: self.show_category(value), relief="flat", bd=0, padx=22, pady=10, bg="white", activebackground="#e6eef5", font=("Microsoft YaHei UI", 11)).pack(side="left")
        for widget in self.content.winfo_children():
            widget.destroy()
        tk.Label(self.content, text=f"{category} · 购买页面", bg="#f3f5f7", fg="#202b36", font=("Microsoft YaHei UI", 17, "bold")).pack(anchor="w", pady=(0, 12))
        canvas = tk.Canvas(self.content, bg="#f3f5f7", highlightthickness=0)
        scrollbar = tk.Scrollbar(self.content, orient="vertical", command=canvas.yview)
        list_frame = tk.Frame(canvas, bg="#f3f5f7")
        list_frame.bind("<Configure>", lambda event: canvas.configure(scrollregion=canvas.bbox("all")))
        canvas.create_window((0, 0), window=list_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        for item in self.data["物品"][category]:
            self.create_item(list_frame, item)
        self.update_balance()

    def create_item(self, parent, item):
        card = tk.Frame(parent, bg="white", highlightbackground="#dce2e7", highlightthickness=1)
        card.pack(fill="x", pady=5)
        tk.Label(card, text=item["名称"], bg="white", fg="#202b36", font=("Microsoft YaHei UI", 13, "bold"), width=18, anchor="w").pack(side="left", padx=14, pady=12)
        details = []
        if "伤害" in item:
            details = [f"{'回血' if item['伤害'] < 0 else '伤害'}：{abs(item['伤害'])}", f"距离：{item['距离']}"]
        tk.Label(card, text="　".join(details) if details else "弹药", bg="white", fg="#52616b", anchor="w").pack(side="left", fill="x", expand=True)
        tk.Label(card, text=item.get("简介", "暂无简介"), bg="white", fg="#7a8791", anchor="w").pack(side="left", padx=12)
        tk.Button(card, text=f"{item['价格']} M币\n购买", command=lambda value=item: self.buy(value), bg="#2a9d8f", fg="white", activebackground="#21867b", relief="flat", bd=0, width=10, font=("Microsoft YaHei UI", 10, "bold")).pack(side="right", padx=12, pady=8)

    def buy(self, item):
        if self.data["M币"] < item["价格"]:
            messagebox.showerror("购买失败", "M币不足。")
            return
        self.data["M币"] -= item["价格"]
        DATA_FILE.write_text(json.dumps(self.data, ensure_ascii=False, indent=2), encoding="utf-8")
        add_to_bag(item["名称"])
        self.update_balance()
        messagebox.showinfo("购买成功", f"已购买：{item['名称']}")


if __name__ == "__main__":
    window = tk.Tk()
    ShopApp(window)
    window.mainloop()
