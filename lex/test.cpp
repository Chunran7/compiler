#include <unordered_map>

using namespace std;

struct Node{
    int val;
    Node* prev;
    Node* next;
};

class LRUCache {
private:
    Node* head,tail;
    unordered_map<int, Node*> m;
    int capacity;
    int size;
public:
    void addHead(Node* node){
        if(node==nullptr) return;
        if(head==nullptr){
            head = node;
            tail = node;
            return;
        }
        head->prev = node;
        node->next = head;
        node->prev = nullptr;
        head = node;
    }
    
    removeNode(Node* node){       
        node->prev->next = node->next;
        node->next->prev = node->prev;
    }


    LRUCache(int capacity) {
        this->capacity = capacity;
        this->size = 0;
        this->head = nullptr;
        this->tail = nullptr;
    }
    
    int get(int key) {
        if(m.count(key)){
            Node* node = m[key];
            removeNode(node);
            addHead(node);
            return node->val;
        }
        else{
            if(capacity==size){
                removeNode(tail);
                m.erase(tail->val);
                size--;
            }
            Node* node = new Node();
            node->val = value;
            m[key] = node;
            addHead(node);
            size++;
            return -1;
        }
    }
    
    void put(int key, int value) {
        if(m.count(key)){
            Node* node = m[key];
            removeNode(node);
            addHead(node);
            return;
        }
        Node* node = new Node();
        node->val = value;
        m[key] = node;
        addHead(node);
        if(m.size()>capacity){
            removeNode(tail);
            m.erase(tail->val);
        }
    }
};

/**
 * Your LRUCache object will be instantiated and called as such:
 * LRUCache* obj = new LRUCache(capacity);
 * int param_1 = obj->get(key);
 * obj->put(key,value);
 */