// http://stackoverflow.com/questions/7792011/alternative-to-boostshared-ptr-in-an-embedded-environment
template<class T>
class my_shared_ptr
{
    template<class U>
    friend class my_shared_ptr;
public:
    my_shared_ptr() :p(), c() {}
    explicit my_shared_ptr(T* s) :p(s), c(new unsigned(1)) {}

    my_shared_ptr(const my_shared_ptr& s) :p(s.p), c(s.c) { if(c) ++*c; }

    my_shared_ptr& operator=(const my_shared_ptr& s)
    { if(this!=&s) { clear(); p=s.p; c=s.c; if(c) ++*c; } return *this; }

    template<class U>
    my_shared_ptr(const my_shared_ptr<U>& s) :p(s.p), c(s.c) { if(c) ++*c; }

    ~my_shared_ptr() { clear(); }

    void clear()
    {
        if(c)
        {
            if(*c==1) delete p;
            if(!--*c) delete c;
        }
        c=0; p=0;
    }

    T* get() const { return (c)? p: 0; }
    T* operator->() const { return get(); }
    T& operator*() const { return *get(); }

private:
    T* p;
    unsigned* c;
};

